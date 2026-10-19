package cloud.amnezia.dtm.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cloud.amnezia.dtm.Recorder
import cloud.amnezia.dtm.RecorderService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RecordScreen() {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val snack = LocalSnackbar.current
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<Recorder.Item>>(emptyList()) }
    var refresh by remember { mutableIntStateOf(0) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var justSaved by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Recorder.Item?>(null) }
    var details by remember { mutableStateOf<Recorder.Item?>(null) }

    LaunchedEffect(Recorder.savedTick, refresh) { items = Recorder.list(context) }
    LaunchedEffect(Recorder.isRecording) {
        while (Recorder.isRecording) {
            elapsed = (System.currentTimeMillis() - Recorder.startedAtMs) / 1000
            delay(500)
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res[Manifest.permission.RECORD_AUDIO] == true) { justSaved = false; RecorderService.start(context) }
        else Recorder.lastError = "Нет доступа к микрофону. Разрешите его в настройках телефона."
    }

    ScreenScaffold(
        title = "Запись",
        subtitle = "Одно касание — и запись идёт, даже при погашенном экране. Каждой записи присваивается цифровой отпечаток SHA-256.",
    ) {
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                RecordButton(Recorder.isRecording) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (Recorder.isRecording) { RecorderService.stop(context); justSaved = true }
                    else launcher.launch(
                        if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
                        else arrayOf(Manifest.permission.RECORD_AUDIO)
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (Recorder.isRecording) {
                    Text(formatDuration(elapsed), style = TimerStyle, color = cs.onSurface)
                    Text("Коснитесь, чтобы остановить", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                } else {
                    Text("Коснитесь, чтобы начать", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                }
                val err = Recorder.lastError
                when {
                    err != null -> Text(err, color = cs.error, style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                    justSaved && !Recorder.isRecording -> Text(STEP_DONE, color = cs.primary,
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        if (items.isEmpty()) {
            item {
                InfoCard(Icons.Outlined.Fingerprint, "Записей пока нет",
                    "Записи хранятся зашифрованно и не попадают в галерею или файловый менеджер. " +
                        "Отпечаток SHA-256 подтверждает, что файл не меняли после записи.")
            }
        } else {
            item { SectionLabel("Сохранённые записи · ${items.size}") }
            items(items, key = { it.sessionId }) { r ->
                val first = r.chunks.first()
                val play: () -> Unit = {
                    scope.launch {
                        val f = runCatching { Recorder.decryptChunk(context, first) }.getOrNull()
                        if (f == null || !openFile(context, f)) snack.showSnackbar("Не удалось открыть запись")
                    }
                }
                ItemCard(
                    headline = prettyDate(r.startUtc),
                    supporting = buildString {
                        if (r.durationSec > 0) append(formatDuration(r.durationSec)).append(" · ")
                        append(formatSize(r.size))
                        if (r.chunks.size > 1) append(" · ${r.chunks.size} ${plural(r.chunks.size, "часть", "части", "частей")}")
                        append(if (r.networkTime) "\n🕑 сетевое время" else "\n🕑 время устройства")
                        if (r.lat != null && r.lon != null) append(" · 📍 есть координаты")
                    },
                    icon = Icons.Filled.GraphicEq,
                    onClick = play,
                    actions = buildList {
                        add(MenuAction("Слушать", Icons.Filled.PlayArrow, onClick = play))
                        add(MenuAction("Поделиться", Icons.Filled.Share) {
                            scope.launch {
                                runCatching {
                                    val files = r.chunks.map { Recorder.decryptChunk(context, it) }
                                    shareFiles(context, files, "Отправить запись")
                                }.onFailure { snack.showSnackbar("Не удалось подготовить файлы") }
                            }
                        })
                        add(MenuAction("Отпечатки и время", Icons.Filled.Info) { details = r })
                        add(MenuAction("Удалить", Icons.Filled.Delete, destructive = true) { toDelete = r })
                    },
                )
            }
        }
    }

    details?.let { r -> RecordDetailsDialog(r) { details = null } }

    toDelete?.let { r ->
        ConfirmDialog(
            "Удалить запись?",
            "Запись от ${prettyDate(r.startUtc)} и все её части будут удалены без возможности восстановления.",
            "Удалить",
            onConfirm = { Recorder.delete(context, r); refresh++ },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
private fun RecordDetailsDialog(item: Recorder.Item, onDismiss: () -> Unit) {
    val context = LocalContext.current
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                val text = buildString {
                    appendLine("Запись от ${prettyDate(item.startUtc)}")
                    appendLine("Время: ${if (item.networkTime) "сетевое (SNTP)" else "устройства"}")
                    if (item.lat != null && item.lon != null) appendLine("Координаты: ${"%.5f".format(item.lat)}, ${"%.5f".format(item.lon)}")
                    appendLine()
                    item.chunks.forEach { c ->
                        appendLine("Часть ${c.part + 1}: ${c.plainName}")
                        appendLine("  SHA-256: ${c.sha256}")
                    }
                }
                copyText(context, "Манифест записи", text)
                onDismiss()
            }) { androidx.compose.material3.Text("Копировать всё") }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { androidx.compose.material3.Text("Закрыть") } },
        title = { androidx.compose.material3.Text("Отпечатки и время") },
        text = {
            androidx.compose.foundation.layout.Column(
                Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                androidx.compose.material3.Text(
                    (if (item.networkTime) "Время получено с сервера точного времени (SNTP) и не зависит от настроек телефона.\n\n"
                    else "Сетевое время получить не удалось, указано время устройства.\n\n") +
                        "Отпечаток SHA-256 у каждой части подтверждает, что файл не меняли после записи:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                item.chunks.forEach { c ->
                    androidx.compose.material3.Text("Часть ${c.part + 1} · ${formatDuration(c.durationSec)}",
                        style = MaterialTheme.typography.titleSmall)
                    androidx.compose.material3.Text(c.sha256,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
    )
}

@Composable
private fun RecordButton(recording: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val container by animateColorAsState(if (recording) cs.tertiary else cs.primaryContainer, label = "c")
    val content by animateColorAsState(if (recording) cs.onTertiary else cs.onPrimaryContainer, label = "cc")
    // Форма «перетекает» из круга в скруглённый квадрат — выразительный стиль Material 3.
    val corner by animateDpAsState(if (recording) 44.dp else 90.dp, label = "corner")
    Box(Modifier.size(232.dp), contentAlignment = Alignment.Center) {
        if (recording) PulseRings(cs.tertiary)
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(corner),
            color = container,
            contentColor = content,
            shadowElevation = 4.dp,
            modifier = Modifier.size(172.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(if (recording) Icons.Filled.Stop else Icons.Filled.Mic,
                    if (recording) "Остановить запись" else "Начать запись", Modifier.size(64.dp))
            }
        }
    }
}

@Composable
private fun PulseRings(color: Color) {
    val t = rememberInfiniteTransition(label = "pulse")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "p")
    Canvas(Modifier.fillMaxSize()) {
        val r = size.minDimension / 2
        for (k in 0..1) {
            val q = (p + k / 2f) % 1f
            drawCircle(color.copy(alpha = (1 - q) * 0.30f), radius = r * (0.74f + 0.26f * q))
        }
    }
}

fun copyText(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
}
