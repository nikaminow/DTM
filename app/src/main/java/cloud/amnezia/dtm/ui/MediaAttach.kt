package cloud.amnezia.dtm.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import cloud.amnezia.dtm.data.Crypto
import cloud.amnezia.dtm.data.JournalMedia
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Панель вложений для события дневника: камера, файл и голосовая заметка. */
@Composable
fun AttachmentsEditor(atts: SnapshotStateList<String>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var capturePath by rememberSaveable { mutableStateOf<String?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recording by remember { mutableStateOf(false) }
    var recFile by remember { mutableStateOf<File?>(null) }

    DisposableEffect(Unit) {
        onDispose { runCatching { recorder?.release() } }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = capturePath; capturePath = null
        if (ok && path != null) {
            val f = File(path)
            if (f.exists() && f.length() > 0) scope.launch {
                atts.add(JournalMedia.importFile(context, f, "Фото " + stamp() + ".jpg"))
            } else f.delete()
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch { atts.add(JournalMedia.importUri(context, uri)) }
    }
    val micPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVoice(context) { rec, file -> recorder = rec; recFile = file; recording = true }
    }

    fun stopVoice() {
        val r = recorder; val f = recFile
        recorder = null; recFile = null; recording = false
        if (r == null || f == null) return
        val ok = runCatching { r.stop() }.isSuccess
        r.runCatching { release() }
        if (ok && f.length() > 0) scope.launch {
            atts.add(JournalMedia.importFile(context, f, "Заметка " + stamp() + ".m4a"))
        } else f.delete()
    }

    Column(Modifier.fillMaxWidth()) {
        Text("Вложения", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AddChip("Камера", Icons.Filled.PhotoCamera) {
                val f = File(Crypto.captureDir(context), "cap_${System.currentTimeMillis()}.jpg")
                capturePath = f.absolutePath
                val uri = FileProvider.getUriForFile(context, "cloud.amnezia.dtm.fileprovider", f)
                runCatching { camera.launch(uri) }
            }
            AddChip("Файл", Icons.Filled.UploadFile) {
                runCatching { picker.launch(arrayOf("image/*", "video/*", "audio/*")) }
            }
            if (recording) {
                AssistChip(onClick = { stopVoice() },
                    label = { Text("Стоп") },
                    leadingIcon = { Icon(Icons.Filled.Stop, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) })
            } else {
                AddChip("Голос", Icons.Filled.Mic) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                        startVoice(context) { rec, file -> recorder = rec; recFile = file; recording = true }
                    else micPerm.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }

        if (atts.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(atts.toList()) { name ->
                    AttChip(JournalMedia.att(context, name)) { atts.remove(name) }
                }
            }
        }
    }
}

@Composable
private fun AddChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label) },
        leadingIcon = { Icon(icon, null, Modifier.size(AssistChipDefaults.IconSize)) })
}

@Composable
private fun AttChip(att: JournalMedia.Att, onRemove: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    val icon = when (att.kind) {
        JournalMedia.Kind.IMAGE -> Icons.Filled.Image
        JournalMedia.Kind.VIDEO -> Icons.Filled.Videocam
        JournalMedia.Kind.AUDIO -> Icons.Filled.AudioFile
        else -> Icons.Filled.InsertDriveFile
    }
    Surface(color = cs.surfaceContainerHigh, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(18.dp), tint = cs.primary)
            Spacer(Modifier.width(6.dp))
            Text(att.display.take(18), style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clickableOpen(scope, context, att))
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(24.dp).clickableRemove(onRemove), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, "Убрать", Modifier.size(16.dp), tint = cs.onSurfaceVariant)
            }
        }
    }
}

private fun Modifier.clickableOpen(scope: kotlinx.coroutines.CoroutineScope, context: android.content.Context, att: JournalMedia.Att) =
    this.clickable {
        scope.launch { runCatching { openFile(context, JournalMedia.decrypt(context, att.name)) } }
    }

private fun Modifier.clickableRemove(onRemove: () -> Unit) =
    this.clickable(onClick = onRemove)

private fun startVoice(context: android.content.Context, onStarted: (MediaRecorder, File) -> Unit) {
    runCatching {
        val file = File(Crypto.captureDir(context), "voice_${System.currentTimeMillis()}.m4a")
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
        else @Suppress("DEPRECATION") MediaRecorder()
        rec.setAudioSource(MediaRecorder.AudioSource.MIC)
        rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        rec.setAudioEncodingBitRate(64_000)
        rec.setAudioSamplingRate(44_100)
        rec.setOutputFile(file.absolutePath)
        rec.prepare()
        rec.start()
        onStarted(rec, file)
    }
}

private fun stamp(): String = SimpleDateFormat("yyyy-MM-dd HH-mm", Locale.US).format(Date())
