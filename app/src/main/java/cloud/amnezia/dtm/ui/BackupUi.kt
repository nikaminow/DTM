package cloud.amnezia.dtm.ui

import cloud.amnezia.dtm.tr

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import cloud.amnezia.dtm.data.Backup
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Создание резервной копии: пароль → выбор места → сохранение. */
@Composable
fun ExportAction(trigger: @Composable (start: () -> Unit) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var askPassword by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
        if (uri == null) { password = ""; return@rememberLauncherForActivityResult }
        scope.launch {
            busy = true
            message = runCatching { Backup.export(context, password, uri) }.fold(
                onSuccess = { tr("Резервная копия сохранена.\n\nХраните файл и пароль в разных местах. Без пароля копию не открыть — восстановить его невозможно.") },
                onFailure = { tr("Не удалось создать копию: %1\$s", it.message) },
            )
            password = ""
            busy = false
        }
    }

    trigger { askPassword = true }

    if (askPassword) PasswordDialog(
        title = tr("Пароль для копии"),
        text = tr("Копия будет зашифрована этим паролем. Запомните его: без пароля восстановить данные нельзя."),
        confirmTwice = true,
        onDismiss = { askPassword = false },
        onConfirm = { pw ->
            askPassword = false
            password = pw
            val name = "DTM_" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) + ".dtmbak"
            runCatching { create.launch(name) }
        },
    )
    if (busy) BusyDialog(tr("Создаю резервную копию…"))
    message?.let { InfoDialog(it) { message = null } }
}

/** Восстановление: выбор файла → пароль → загрузка данных. */
@Composable
fun RestoreAction(onRestored: () -> Unit, trigger: @Composable (start: () -> Unit) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var picked by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }

    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> picked = uri }

    trigger { runCatching { open.launch(arrayOf("*/*")) } }

    picked?.let { uri ->
        PasswordDialog(
            title = tr("Пароль от копии"),
            text = tr("Введите пароль, который вы задали при создании копии. Данные из копии добавятся к текущим, совпадающие будут заменены."),
            confirmTwice = false,
            onDismiss = { picked = null },
            onConfirm = { pw ->
                picked = null
                scope.launch {
                    busy = true
                    val r = runCatching { Backup.restore(context, pw, uri) }
                    busy = false
                    success = r.isSuccess
                    message = r.fold(
                        onSuccess = { tr("Данные восстановлены. Приложение перезапустится — войдите с PIN-кодом из копии.") },
                        onFailure = { it.message ?: tr("Не удалось восстановить") },
                    )
                }
            },
        )
    }
    if (busy) BusyDialog(tr("Восстанавливаю данные…"))
    message?.let { InfoDialog(it) { message = null; if (success) onRestored() } }
}

@Composable
private fun PasswordDialog(
    title: String, text: String, confirmTwice: Boolean,
    onDismiss: () -> Unit, onConfirm: (String) -> Unit,
) {
    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    val valid = if (confirmTwice) p1.length >= 6 && p1 == p2 else p1.isNotEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text, style = MaterialTheme.typography.bodyMedium)
                PwField(p1, { p1 = it }, if (confirmTwice) tr("Пароль (от 6 символов)") else tr("Пароль"))
                if (confirmTwice) {
                    PwField(p2, { p2 = it }, tr("Повторите пароль"))
                    if (p2.isNotEmpty() && p1 != p2) Text(tr("Пароли не совпадают"), color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onConfirm(p1) }) { Text(tr("Продолжить")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Отмена")) } },
    )
}

@Composable
private fun PwField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun BusyDialog(text: String) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        confirmButton = {},
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(28.dp))
                Spacer(Modifier.width(16.dp))
                Text(text)
            }
        },
    )
}

@Composable
private fun InfoDialog(text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Понятно")) } },
    )
}
