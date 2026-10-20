package cloud.amnezia.dtm.data

import cloud.amnezia.dtm.tr

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import cloud.amnezia.dtm.MainActivity
import cloud.amnezia.dtm.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class CalEvent(val id: Long, val time: Long, val title: String, val note: String, val remindMin: Int)

object CalendarStore {
    val REMIND = listOf(-1 to tr("Без напоминания"), 0 to tr("В момент события"), 15 to tr("За 15 минут"), 60 to tr("За час"), 1440 to tr("За день"))

    private val lock = Mutex()
    private fun file(context: Context) = File(context.filesDir, "calendar.enc")

    suspend fun load(context: Context): List<CalEvent> = withContext(Dispatchers.IO) {
        val raw = runCatching { Crypto.readText(context, file(context)) }.getOrNull() ?: return@withContext emptyList()
        runCatching {
            val a = JSONArray(raw)
            (0 until a.length()).map { a.getJSONObject(it) }.map {
                CalEvent(it.getLong("id"), it.getLong("t"), it.optString("title"), it.optString("note"), it.optInt("r", -1))
            }.sortedBy { it.time }
        }.getOrDefault(emptyList())
    }

    private suspend fun write(context: Context, list: List<CalEvent>) = withContext(Dispatchers.IO) {
        val a = JSONArray()
        list.forEach { a.put(JSONObject().put("id", it.id).put("t", it.time).put("title", it.title).put("note", it.note).put("r", it.remindMin)) }
        Crypto.writeText(context, file(context), a.toString())
    }

    suspend fun upsert(context: Context, e: CalEvent) = lock.withLock {
        write(context, load(context).filter { it.id != e.id } + e)
        Reminders.schedule(context, e)
    }

    suspend fun delete(context: Context, e: CalEvent) = lock.withLock {
        write(context, load(context).filter { it.id != e.id })
        Reminders.cancel(context, e.id)
    }
}

object Reminders {
    private const val CHANNEL = "dtm_reminders"

    private fun intent(context: Context, id: Long, title: String = "", min: Int = 0): PendingIntent =
        PendingIntent.getBroadcast(
            context, (id % Int.MAX_VALUE).toInt(),
            Intent(context, ReminderReceiver::class.java).putExtra("id", id).putExtra("title", title).putExtra("min", min),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    fun schedule(context: Context, e: CalEvent) {
        cancel(context, e.id)
        if (e.remindMin < 0) return
        val at = e.time - e.remindMin * 60_000L
        if (at <= System.currentTimeMillis()) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = intent(context, e.id, e.title, e.remindMin)
        runCatching {
            if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancel(context: Context, id: Long) {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(intent(context, id))
    }

    suspend fun rescheduleAll(context: Context) {
        CalendarStore.load(context).forEach { schedule(context, it) }
    }

    fun show(context: Context, id: Long, title: String, min: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL, tr("Напоминания"), NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 7, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val whenText = when (min) { 0 -> tr("Сейчас"); 15 -> tr("Через 15 минут"); 60 -> tr("Через час"); 1440 -> tr("Завтра"); else -> tr("Скоро") }
        val public = Notification.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_stat_bell)
            .setContentTitle("DTM").setContentText(tr("Напоминание")).build()
        val n = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_bell)
            .setContentTitle(title.ifBlank { tr("Напоминание") })
            .setContentText(whenText)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(public)
            .build()
        runCatching { nm.notify((id % Int.MAX_VALUE).toInt(), n) }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.show(context, intent.getLongExtra("id", 0), intent.getStringExtra("title").orEmpty(), intent.getIntExtra("min", 0))
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { Reminders.rescheduleAll(context.applicationContext) }
            pending.finish()
        }
    }
}
