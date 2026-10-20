@file:OptIn(ExperimentalMaterial3Api::class)

package cloud.amnezia.dtm.ui

import cloud.amnezia.dtm.tr

import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Поддерживающая фраза после важного действия — без указания рода. */
val STEP_DONE = tr("Это было непросто, но шаг сделан")

val LocalSnackbar = staticCompositionLocalOf<SnackbarHostState> { error("no snackbar") }

/** Экран с большим сворачиваемым заголовком Material 3 и прокручиваемым списком. */
@Composable
fun ScreenScaffold(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    bottomSpace: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = if (bottomSpace) 190.dp else 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 6.dp)) {
                androidx.compose.foundation.layout.Row(
                    Modifier.fillMaxWidth().height(52.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    if (onBack != null) {
                        Box(
                            Modifier.size(46.dp).softClickable(onClick = onBack)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(cs.surfaceContainerHigh),
                            contentAlignment = androidx.compose.ui.Alignment.Center,
                        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Назад")) }
                    }
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    actions()
                }
                Text(title, style = MaterialTheme.typography.headlineLarge, color = cs.onSurface,
                    modifier = Modifier.padding(top = 6.dp))
                if (subtitle != null) Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, end = 8.dp),
                )
            }
        }
        content()
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
    )
}

data class MenuAction(val label: String, val icon: ImageVector, val destructive: Boolean = false, val onClick: () -> Unit)

/** Карточка-строка списка с меню действий. */
@Composable
fun ItemCard(
    headline: String,
    supporting: String,
    icon: ImageVector,
    onClick: () -> Unit,
    actions: List<MenuAction>,
    overline: String? = null,
) {
    var menu by remember { mutableStateOf(false) }
    val cs = MaterialTheme.colorScheme
    SoftCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), onClick = onClick, padding = 14.dp) {
        androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            SoftIcon(icon)
            androidx.compose.foundation.layout.Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                if (overline != null) Text(overline, style = MaterialTheme.typography.labelMedium, color = cs.primary, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(headline, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (supporting.isNotBlank()) Text(supporting, style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Box {
                Box(
                    Modifier.size(40.dp).softClickable { menu = true },
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) { Icon(Icons.Filled.MoreVert, tr("Действия"), tint = cs.onSurfaceVariant) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    actions.forEach { a ->
                        val c = if (a.destructive) cs.error else cs.onSurface
                        DropdownMenuItem(
                            text = { Text(a.label, color = c) },
                            leadingIcon = { Icon(a.icon, null, tint = c) },
                            onClick = { menu = false; a.onClick() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IconBadge(icon: ImageVector, container: Color = MaterialTheme.colorScheme.secondaryContainer,
              content: Color = MaterialTheme.colorScheme.onSecondaryContainer) {
    SoftIcon(icon, container, content)
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirm, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Отмена")) } },
    )
}

// ---------- Файлы ----------

private const val AUTHORITY = "cloud.amnezia.dtm.fileprovider"

fun mimeOf(file: File): String =
    MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "application/octet-stream"

fun openFile(context: Context, file: File): Boolean {
    val uri = FileProvider.getUriForFile(context, AUTHORITY, file)
    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mimeOf(file))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    return try { context.startActivity(intent); true } catch (_: Exception) { false }
}

fun shareFiles(context: Context, files: List<File>, title: String = tr("Отправить")) {
    if (files.size == 1) { shareFile(context, files[0], title); return }
    val uris = ArrayList(files.map { FileProvider.getUriForFile(context, AUTHORITY, it) })
    val send = Intent(Intent.ACTION_SEND_MULTIPLE).setType("audio/mp4")
        .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun shareFile(context: Context, file: File, title: String = tr("Отправить")) {
    val uri = FileProvider.getUriForFile(context, AUTHORITY, file)
    val send = Intent(Intent.ACTION_SEND).setType(mimeOf(file))
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

// ---------- Форматирование ----------

private val RU = java.util.Locale.getDefault()

fun prettyDate(ms: Long): String = SimpleDateFormat("d MMMM yyyy, HH:mm", RU).format(Date(ms))
fun shortDate(ms: Long): String = SimpleDateFormat("d MMM, HH:mm", RU).format(Date(ms))
fun dayTitle(ms: Long): String = SimpleDateFormat("d MMMM yyyy, EEEE", RU).format(Date(ms))

fun formatDuration(sec: Long): String =
    if (sec >= 3600) "%d:%02d:%02d".format(sec / 3600, sec % 3600 / 60, sec % 60)
    else "%02d:%02d".format(sec / 60, sec % 60)

fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000 -> tr("%.1f МБ").format(bytes / 1_000_000.0)
    bytes >= 1_000 -> tr("%.0f КБ").format(bytes / 1_000.0)
    else -> tr("%1\$s Б", bytes)
}

fun plural(n: Int, one: String, few: String, many: String): String {
    val m10 = n % 10; val m100 = n % 100
    return when {
        m10 == 1 && m100 != 11 -> one
        m10 in 2..4 && m100 !in 12..14 -> few
        else -> many
    }
}
