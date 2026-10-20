package cloud.amnezia.dtm

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import cloud.amnezia.dtm.audio.NetworkTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Запись звука как foreground-сервис. Ведётся кусками по [Recorder.CHUNK_MS]:
 * при достижении лимита MediaRecorder перекатывается на следующий файл
 * (setNextOutputFile), а завершённый кусок сразу шифруется. При жёстком
 * выключении телефона теряется только текущий кусок.
 */
class RecorderService : Service() {

    private var recorder: MediaRecorder? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var sessionId = 0L
    private var writingPart = 0
    private var chunkStartUtc = 0L
    private var lat: Double? = null
    private var lon: Double? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
            else -> if (recorder == null) stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun start() {
        if (Recorder.isRecording) return
        Recorder.lastError = null
        scope.launch { NetworkTime.sync() }
        try {
            startForegroundCompat()
        } catch (e: Exception) {
            Recorder.lastError = tr("Фоновая запись недоступна, держите приложение открытым")
        }
        sessionId = System.currentTimeMillis()
        writingPart = 0
        captureLocation()
        val first = Recorder.tempChunkFile(this, sessionId, 0)
        try {
            val rec = build()
            rec.setOutputFile(first.absolutePath)
            rec.setOnInfoListener { _, what, _ -> onRecorderInfo(what) }
            rec.prepare()
            rec.start()
            recorder = rec
            chunkStartUtc = NetworkTime.now()
            Recorder.startedAtMs = System.currentTimeMillis()
            Recorder.isRecording = true
        } catch (e: Exception) {
            recorder?.runCatching { release() }
            recorder = null
            first.delete()
            Recorder.isRecording = false
            Recorder.lastError = tr("Не удалось начать запись: %1\$s", e.message ?: e.javaClass.simpleName)
            finish()
        }
    }

    private fun build(): MediaRecorder {
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this)
        else @Suppress("DEPRECATION") MediaRecorder()
        rec.setAudioSource(MediaRecorder.AudioSource.MIC)
        rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        rec.setAudioEncodingBitRate(64_000)
        rec.setAudioSamplingRate(44_100)
        rec.setAudioChannels(1)
        return rec
    }

    private fun trySetNext(part: Int) {
        runCatching {
            recorder?.setNextOutputFile(Recorder.tempChunkFile(this, sessionId, part))
        }
    }

    /** Перекат на следующий файл: предыдущий кусок готов — шифруем его. */
    private fun onRecorderInfo(what: Int) {
        when (what) {
            MediaRecorder.MEDIA_RECORDER_INFO_NEXT_OUTPUT_FILE_STARTED -> {
                val finished = writingPart
                val start = chunkStartUtc
                val end = NetworkTime.now()
                val plain = Recorder.tempChunkFile(this, sessionId, finished)
                val la = lat; val lo = lon
                scope.launch {
                    runCatching { Recorder.finalizeChunk(applicationContext, plain, sessionId, finished, start, end, la, lo) }
                }
                writingPart += 1
                chunkStartUtc = NetworkTime.now()
                trySetNext(writingPart + 1)
            }
            MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED -> {
                // Если следующий файл не был принят, останавливаемся штатно.
            }
        }
    }

    private fun stop() {
        val rec = recorder ?: run { finish(); return }
        val part = writingPart
        val start = chunkStartUtc
        val plain = Recorder.tempChunkFile(this, sessionId, part)
        val la = lat; val lo = lon
        val ok = try { rec.stop(); true } catch (_: Exception) { false }
        rec.runCatching { reset() }
        rec.runCatching { release() }
        recorder = null
        Recorder.isRecording = false
        // Удаляем заготовленный, но неиспользованный следующий файл.
        Recorder.tempChunkFile(this, sessionId, part + 1).delete()
        scope.launch {
            runCatching {
                if (ok && plain.length() > 0)
                    Recorder.finalizeChunk(applicationContext, plain, sessionId, part, start, NetworkTime.now(), la, lo)
                else plain.delete()
            }.onFailure { Recorder.lastError = tr("Не удалось зашифровать запись: %1\$s", it.message) }
            finish()
        }
    }

    private fun finish() {
        try { stopForeground(STOP_FOREGROUND_REMOVE) } catch (_: Exception) {}
        stopSelf()
    }

    override fun onDestroy() {
        recorder?.let { r ->
            r.runCatching { stop() }
            r.runCatching { release() }
            recorder = null
            Recorder.isRecording = false
        }
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")
    private fun captureLocation() {
        lat = null; lon = null
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return
        runCatching {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc: Location? = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
                .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
                .maxByOrNull { it.time }
            if (loc != null) { lat = loc.latitude; lon = loc.longitude }
        }
    }

    private fun startForegroundCompat() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, tr("Запись"), NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, RecorderService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        // Нейтральный текст: уведомление не выдаёт назначение приложения.
        val notif = Notification.Builder(this, CHANNEL)
            .setContentTitle(tr("Диктофон"))
            .setContentText(tr("Идёт запись"))
            .setSmallIcon(R.drawable.ic_stat_rec)
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, tr("Остановить"), stop).build())
            .setOngoing(true)
            .setVisibility(Notification.VISIBILITY_SECRET)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(1, notif)
        }
    }

    companion object {
        private const val CHANNEL = "dtm_recorder"
        const val ACTION_START = "cloud.amnezia.dtm.START"
        const val ACTION_STOP = "cloud.amnezia.dtm.STOP"

        fun start(context: Context) {
            try {
                context.startForegroundService(Intent(context, RecorderService::class.java).setAction(ACTION_START))
            } catch (e: Exception) {
                Recorder.lastError = tr("Не удалось запустить запись: %1\$s", e.message)
            }
        }

        fun stop(context: Context) {
            try {
                context.startService(Intent(context, RecorderService::class.java).setAction(ACTION_STOP))
            } catch (e: Exception) {
                Recorder.lastError = tr("Не удалось остановить запись: %1\$s", e.message)
            }
        }
    }
}
