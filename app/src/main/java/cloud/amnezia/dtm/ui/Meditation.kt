package cloud.amnezia.dtm.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cloud.amnezia.dtm.data.Tracker
import cloud.amnezia.dtm.tr
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private object Noise {
    @Volatile private var running = false
    private var thread: Thread? = null

    fun start(kind: Int, volume: Float) {
        stop()
        running = true
        thread = Thread {
            val rate = 22050
            val buf = ShortArray(2048)
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(buf.size * 4).setTransferMode(AudioTrack.MODE_STREAM).build()
            track.play()
            var b0 = 0.0; var b1 = 0.0; var b2 = 0.0; var brown = 0.0
            var fade = 0.0
            while (running) {
                for (i in buf.indices) {
                    val w = Random.nextDouble(-1.0, 1.0)
                    val s = when (kind) {
                        1 -> { b0 = 0.99765 * b0 + w * 0.0990460; b1 = 0.96300 * b1 + w * 0.2965164; b2 = 0.57000 * b2 + w * 1.0526913; (b0 + b1 + b2 + w * 0.1848) * 0.18 }
                        2 -> { brown = (brown + 0.02 * w) / 1.02; brown * 3.2 }
                        else -> w * 0.5
                    }
                    if (fade < 1.0) fade += 1.0 / (rate * 2)
                    buf[i] = (s.coerceIn(-1.0, 1.0) * volume * fade * Short.MAX_VALUE).toInt().toShort()
                }
                track.write(buf, 0, buf.size)
            }
            track.stop(); track.release()
        }.apply { start() }
    }

    fun stop() {
        running = false
        thread?.join(500)
        thread = null
    }
}

@Composable
fun MeditationScreen(onBack: () -> Unit, openVoice: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    val kinds = listOf(tr("Белый шум"), tr("Розовый шум"), tr("Коричневый шум"))
    val mins = listOf(10, 20, 30, 60)
    var kind by remember { mutableIntStateOf(1) }
    var minutes by remember { mutableIntStateOf(20) }
    var playing by remember { mutableStateOf(false) }
    var left by remember { mutableIntStateOf(0) }

    var limit by remember { mutableIntStateOf(0) }
    var limitDone by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { Noise.stop() } }

    LaunchedEffect(playing, kind) {
        if (!playing) { Noise.stop(); return@LaunchedEffect }
        Noise.start(kind, 0.6f)
        left = minutes * 60
        while (left > 0 && playing) { delay(1000); left-- }
        if (playing) {
            playing = false
            Tracker.addLog(context, tr("Медитация"), "${kinds[kind]}, ${minutes} " + tr("мин"))
        }
    }
    LaunchedEffect(limit) {
        if (limit <= 0) return@LaunchedEffect
        limitDone = false
        while (limit > 0) { delay(1000); limit-- }
        limitDone = true
        Tracker.addLog(context, tr("Лимит размышлений"), tr("5 минут"))
    }

    ScreenScaffold(
        title = tr("Медитации"),
        subtitle = tr("Звуки для расслабления и сна, а также практики против мысленной жвачки."),
        onBack = onBack, bottomSpace = false,
    ) {
        item {
            SoftCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                Text(tr("Шум для успокоения"), style = MaterialTheme.typography.titleLarge)
                Text(tr("Ровный звук маскирует шумы вокруг и помогает расслабиться или уснуть. Звук нарастает мягко."),
                    style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    kinds.forEachIndexed { i, k -> SoftChip(k, kind == i) { kind = i } }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    mins.forEach { m -> SoftChip("$m " + tr("мин"), minutes == m) { if (!playing) minutes = m } }
                }
                Box(Modifier.fillMaxWidth().padding(top = 20.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.size(110.dp).clip(CircleShape).background(if (playing) cs.tertiary else cs.primary)
                            .softClickable { playing = !playing },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, null,
                            tint = if (playing) cs.onTertiary else cs.onPrimary, modifier = Modifier.size(52.dp))
                    }
                }
                Text(
                    if (playing) formatDuration(left.toLong()) else tr("Нажмите, чтобы включить"),
                    Modifier.fillMaxWidth().padding(top = 10.dp), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        item {
            SoftCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), color = cs.secondaryContainer, contentColor = cs.onSecondaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SoftIcon(Icons.Outlined.Timer, cs.secondary, cs.onSecondary, 44.dp)
                    Spacer(Modifier.size(12.dp))
                    Text(tr("Критерий достаточности"), style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(8.dp))
                Text(tr("Мысль крутится по кругу? Дайте ей ровно 5 минут. Когда время выйдет — базовый вывод сделан, больше ничего не перепроверяете. Часть неопределённости остаётся, и это нормально."),
                    style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                when {
                    limit > 0 -> Text(tr("Осталось: %1\$s", formatDuration(limit.toLong())), style = MaterialTheme.typography.headlineSmall)
                    limitDone -> Text(tr("Стоп. Об этом подумано достаточно. Неопределённость можно выдержать — возвращайтесь к своим делам."),
                        style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(10.dp))
                PillButton(if (limit > 0) tr("Остановить") else tr("Дать мысли 5 минут"),
                    { if (limit > 0) { limit = 0; limitDone = false } else limit = 300 },
                    Modifier.fillMaxWidth(), style = PillStyle.Primary)
            }
        }
        item {
            SoftCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), onClick = openVoice) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SoftIcon(Icons.Outlined.RecordVoiceOver, size = 44.dp)
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr("Голосовые медитации"), style = MaterialTheme.typography.titleMedium)
                        Text(tr("От руминаций, прощение себя, заземление, дыхание"), style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
