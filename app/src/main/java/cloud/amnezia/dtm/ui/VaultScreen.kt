package cloud.amnezia.dtm.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import cloud.amnezia.dtm.data.Crypto
import cloud.amnezia.dtm.data.Vault
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VaultScreen() {
    val context = LocalContext.current
    val snack = LocalSnackbar.current
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf<List<Vault.Item>?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Vault.Item?>(null) }
    var details by remember { mutableStateOf<Vault.Item?>(null) }
    var capturePath by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(refresh) { items = Vault.list(context) }

    fun done(n: Int) {
        refresh++
        scope.launch { snack.showSnackbar(if (n == 1) STEP_DONE else "Добавлено: $n. $STEP_DONE") }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            var ok = 0
            uris.forEach { runCatching { Vault.importUri(context, it) }.onSuccess { ok++ } }
            busy = false
            if (ok > 0) done(ok) else snack.showSnackbar("Не удалось добавить файлы")
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val path = capturePath ?: return@rememberLauncherForActivityResult
        capturePath = null
        val f = File(path)
        if (!success || !f.exists() || f.length() == 0L) { f.delete(); return@rememberLauncherForActivityResult }
        scope.launch {
            busy = true
            val name = "Фото " + SimpleDateFormat("yyyy-MM-dd HH-mm", Locale.US).format(Date()) + ".jpg"
            val loc = runCatching { cloud.amnezia.dtm.Sos.location(context) }.getOrNull()
            val r = runCatching { Vault.importFile(context, f, name, loc?.latitude, loc?.longitude) }
            busy = false
            if (r.isSuccess) done(1) else snack.showSnackbar("Не удалось сохранить фото")
        }
    }

    ScreenScaffold(
        title = "Сейф",
        subtitle = "Документы, фото повреждений, скриншоты переписки. Всё шифруется AES-256 и не попадает в галерею.",
    ) {
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = { picker.launch(arrayOf("*/*")) }, enabled = !busy,
                    modifier = Modifier.weight(1f).height(56.dp)) {
                    Icon(Icons.Filled.UploadFile, null); Spacer(Modifier.width(8.dp)); Text("Файлы")
                }
                FilledTonalButton(
                    onClick = {
                        val f = File(Crypto.captureDir(context), "photo_${System.currentTimeMillis()}.jpg")
                        capturePath = f.absolutePath
                        val uri = FileProvider.getUriForFile(context, "cloud.amnezia.dtm.fileprovider", f)
                        try { camera.launch(uri) } catch (_: Exception) {
                            scope.launch { snack.showSnackbar("Камера недоступна") }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.weight(1f).height(56.dp),
                ) { Icon(Icons.Filled.PhotoCamera, null); Spacer(Modifier.width(8.dp)); Text("Камера") }
            }
        }
        if (busy) item { LinearProgressIndicator(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) }

        val list = items
        if (list != null && list.isEmpty()) item {
            InfoCard(Icons.Outlined.Lock, "Пока пусто",
                "Паспорт, свидетельства, полисы, медицинские справки, фото повреждений, скриншоты угроз — всё, что важно сохранить. " +
                    "Снимки с камеры сразу попадают сюда, минуя галерею.")
        }
        if (!list.isNullOrEmpty()) {
            item { SectionLabel("Файлы · ${list.size}") }
            items(list, key = { it.file.name }) { d ->
                val open: () -> Unit = {
                    scope.launch {
                        val f = runCatching { Vault.decrypt(context, d) }.getOrNull()
                        if (f == null || !openFile(context, f)) snack.showSnackbar("Нет приложения, чтобы открыть этот файл")
                    }
                }
                ItemCard(
                    headline = d.name,
                    supporting = "${shortDate(d.added)} · ${formatSize(d.size)}",
                    icon = when { d.isImage -> Icons.Filled.Image; d.isPdf -> Icons.Filled.PictureAsPdf; else -> Icons.Filled.Description },
                    onClick = open,
                    actions = buildList {
                        add(MenuAction("Открыть", Icons.Filled.OpenInNew, onClick = open))
                        add(MenuAction("Поделиться", Icons.Filled.Share) {
                            scope.launch {
                                runCatching { shareFile(context, Vault.decrypt(context, d), "Отправить файл") }
                                    .onFailure { snack.showSnackbar("Не удалось подготовить файл") }
                            }
                        })
                        if (Vault.stamp(context, d) != null) add(MenuAction("Подлинность", Icons.Filled.VerifiedUser) { details = d })
                        add(MenuAction("Удалить", Icons.Filled.Delete, destructive = true) { toDelete = d })
                    },
                )
            }
        }
    }

    toDelete?.let { d ->
        ConfirmDialog("Удалить файл?", "«${d.name}» будет удалён без возможности восстановления.", "Удалить",
            onConfirm = { Vault.delete(d); refresh++ }, onDismiss = { toDelete = null })
    }

    details?.let { d ->
        val st = Vault.stamp(context, d)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { details = null },
            title = { Text("Подлинность фото") },
            text = {
                androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Снято ${prettyDate(st?.time ?: d.added)}", style = MaterialTheme.typography.bodyMedium)
                    Text(if (st?.networkTime == true) "Время получено из сети (SNTP) — его нельзя подделать настройками телефона."
                        else "Время устройства (сеть была недоступна).", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (st?.lat != null && st.lon != null) Text("Координаты: ${"%.5f".format(st.lat)}, ${"%.5f".format(st.lon)}",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("SHA-256:", style = MaterialTheme.typography.bodyMedium)
                    Text(st?.sha256.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Совпадение отпечатка у переданного файла доказывает, что снимок не редактировали после съёмки.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { copyText(context, "SHA-256", st?.sha256.orEmpty()); details = null }) { Text("Копировать отпечаток") }
            },
            dismissButton = { TextButton(onClick = { details = null }) { Text("Закрыть") } },
        )
    }
}
