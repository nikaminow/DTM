package cloud.amnezia.dtm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import cloud.amnezia.dtm.BuildConfig
import cloud.amnezia.dtm.data.Updater
import kotlinx.coroutines.launch
import java.io.File

/** Проверка обновлений на GitHub: найти → показать что нового → скачать → установить. */
@Composable
fun UpdateAction(trigger: @Composable (start: () -> Unit) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var found by remember { mutableStateOf<Updater.Release?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var ready by remember { mutableStateOf<File?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    trigger {
        scope.launch {
            checking = true
            val r = runCatching { Updater.check() }
            checking = false
            r.onSuccess { rel ->
                if (rel == null) message = "У вас последняя версия (${BuildConfig.VERSION_NAME})." else found = rel
            }.onFailure { message = "Не удалось проверить обновления. Проверьте интернет.\n\n${it.message}" }
        }
    }

    fun installOrAsk(apk: File) {
        if (Updater.canInstall(context)) Updater.install(context, apk)
        else {
            ready = apk
            message = "Чтобы установить обновление, разрешите DTM установку приложений. " +
                "Откроются настройки — включите переключатель и вернитесь, затем нажмите «Проверить обновления» ещё раз."
            Updater.openInstallPermission(context)
        }
    }

    if (checking) AlertDialog(
        onDismissRequest = {}, confirmButton = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        text = { Text("Проверяю обновления…") },
    )

    found?.let { rel ->
        AlertDialog(
            onDismissRequest = { found = null },
            title = { Text("Доступна версия ${rel.version}") },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Сейчас установлена ${BuildConfig.VERSION_NAME}. Данные сохранятся.",
                        style = MaterialTheme.typography.bodyMedium)
                    if (rel.notes.isNotBlank()) Text(rel.notes, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    found = null
                    val cached = ready
                    if (cached != null && cached.exists()) { installOrAsk(cached); return@TextButton }
                    scope.launch {
                        downloading = true; progress = 0f
                        val r = runCatching { Updater.download(context, rel) { progress = it } }
                        downloading = false
                        r.onSuccess { installOrAsk(it) }
                            .onFailure { message = "Не удалось скачать обновление: ${it.message}" }
                    }
                }) { Text("Скачать и установить") }
            },
            dismissButton = { TextButton(onClick = { found = null }) { Text("Позже") } },
        )
    }

    if (downloading) AlertDialog(
        onDismissRequest = {}, confirmButton = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text("Скачиваю обновление") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Text("${(progress * 100).toInt()}%")
            }
        },
    )

    message?.let {
        AlertDialog(
            onDismissRequest = { message = null },
            text = { Text(it) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("Понятно") } },
        )
    }
}
