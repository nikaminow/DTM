@file:OptIn(ExperimentalMaterial3Api::class)

package cloud.amnezia.dtm.ui

import cloud.amnezia.dtm.tr

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cloud.amnezia.dtm.Recorder
import cloud.amnezia.dtm.data.Crypto
import cloud.amnezia.dtm.data.Journal
import cloud.amnezia.dtm.data.JournalEntry
import cloud.amnezia.dtm.data.Vault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun JournalScreen() {
    val context = LocalContext.current
    val snack = LocalSnackbar.current
    val scope = rememberCoroutineScope()

    var entries by remember { mutableStateOf<List<JournalEntry>?>(null) }
    var editing by remember { mutableStateOf<JournalEntry?>(null) }
    var toDelete by remember { mutableStateOf<JournalEntry?>(null) }
    var reportAsk by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        entries = Journal.load(context)
        if (NavFlags.openJournalEditor) {
            NavFlags.openJournalEditor = false
            editing = newEntry()
        }
    }

    fun persist(list: List<JournalEntry>) {
        val sorted = list.sortedByDescending { it.time }
        entries = sorted
        scope.launch { Journal.save(context, sorted) }
    }

    val list = entries.orEmpty()

    ScreenScaffold(
        title = tr("Дневник"),
        subtitle = tr("Фиксируйте, что произошло, когда и кто это видел. Записи с датой помогают доверять своей памяти и пригодятся юристу или в полиции."),
        actions = {
            IconButton(onClick = { reportAsk = true }) { Icon(Icons.Outlined.Description, tr("Сформировать отчёт")) }
        },
    ) {
        item {
            FilledTonalButton(
                onClick = { editing = newEntry() },
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(56.dp),
            ) { Icon(Icons.AutoMirrored.Filled.NoteAdd, null); Spacer(Modifier.width(8.dp)); Text(tr("Новое событие")) }
        }
        if (entries != null && list.isEmpty()) item {
            InfoCard(Icons.Outlined.EditNote, tr("Пока пусто"),
                tr("Опишите событие своими словами — даже коротко. Можно указать прошедшую дату. ") +
                    tr("Записи шифруются и видны только после ввода PIN-кода."))
        }
        val byDay = list.groupBy { dayKey(it.time) }
        byDay.forEach { (_, dayEntries) ->
            item(key = "d" + dayKey(dayEntries.first().time)) { SectionLabel(dayTitle(dayEntries.first().time)) }
            items(dayEntries, key = { it.id }) { e ->
                ItemCard(
                    overline = e.kind + " · " + SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(Date(e.time)),
                    headline = e.text.ifBlank { tr("Без описания") },
                    supporting = buildString {
                        append(if (e.witnesses.isNotBlank()) tr("Свидетели: %1\$s", e.witnesses) else tr("Свидетелей не указано"))
                        if (e.attachments.isNotEmpty()) append(" · 📎 ${e.attachments.size}")
                    },
                    icon = Icons.Filled.EditNote,
                    onClick = { editing = e },
                    actions = listOf(
                        MenuAction(tr("Изменить"), Icons.Filled.Edit) { editing = e },
                        MenuAction(tr("Удалить"), Icons.Filled.Delete, destructive = true) { toDelete = e },
                    ),
                )
            }
        }
    }

    editing?.let { e ->
        EntryEditor(
            initial = e,
            isNew = list.none { it.id == e.id },
            onSave = { ne ->
                persist(list.filter { it.id != ne.id } + ne)
                editing = null
                scope.launch { snack.showSnackbar(STEP_DONE) }
            },
            onDismiss = { editing = null },
        )
    }

    toDelete?.let { e ->
        ConfirmDialog(tr("Удалить событие?"), tr("Запись будет удалена без возможности восстановления."), tr("Удалить"),
            onConfirm = { persist(list.filter { it.id != e.id }) }, onDismiss = { toDelete = null })
    }

    if (reportAsk) AlertDialog(
        onDismissRequest = { reportAsk = false },
        icon = { Icon(Icons.Filled.Share, null) },
        title = { Text(tr("Отчёт для юриста или полиции")) },
        text = {
            Text(tr("Будет собран текстовый отчёт: все события дневника, список записей с отпечатками SHA-256 и список документов сейфа. ") +
                tr("Отчёт передаётся в выбранное приложение в открытом виде — отправляйте его только тем, кому доверяете."))
        },
        confirmButton = {
            TextButton(onClick = {
                reportAsk = false
                scope.launch {
                    val f = withContext(Dispatchers.IO) { buildReport(context, list) }
                    shareFile(context, f, tr("Отправить отчёт"))
                }
            }) { Text(tr("Сформировать")) }
        },
        dismissButton = { TextButton(onClick = { reportAsk = false }) { Text(tr("Отмена")) } },
    )
}

private fun newEntry() = JournalEntry(System.currentTimeMillis(), System.currentTimeMillis(), Journal.KINDS.first(), "")

private fun dayKey(ms: Long): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(ms))

@Composable
private fun EntryEditor(initial: JournalEntry, isNew: Boolean, onSave: (JournalEntry) -> Unit, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var kind by remember { mutableStateOf(initial.kind) }
    var text by remember { mutableStateOf(initial.text) }
    var witnesses by remember { mutableStateOf(initial.witnesses) }
    var time by remember { mutableLongStateOf(initial.time) }
    val atts = remember { androidx.compose.runtime.mutableStateListOf<String>().apply { addAll(initial.attachments) } }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
                .padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(if (isNew) tr("Новое событие") else tr("Событие"), style = MaterialTheme.typography.headlineSmall)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { pickDate = true }, label = { Text(SimpleDateFormat("d MMMM yyyy", java.util.Locale.getDefault()).format(Date(time))) },
                    leadingIcon = { Icon(Icons.Filled.CalendarToday, null) })
                AssistChip(onClick = { pickTime = true }, label = { Text(SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(Date(time))) },
                    leadingIcon = { Icon(Icons.Filled.Schedule, null) })
            }

            Text(tr("Что это было"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Journal.KINDS.forEach { k -> FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k) }) }
            }

            OutlinedTextField(
                value = text, onValueChange = { text = it },
                label = { Text(tr("Что произошло")) },
                placeholder = { Text(tr("Слова, действия, угрозы. Как вы себя чувствовали.")) },
                minLines = 5, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = witnesses, onValueChange = { witnesses = it },
                label = { Text(tr("Свидетели, если были")) },
                modifier = Modifier.fillMaxWidth(),
            )
            AttachmentsEditor(atts)
            Button(
                onClick = { onSave(initial.copy(time = time, kind = kind, text = text.trim(), witnesses = witnesses.trim(), attachments = atts.toList())) },
                enabled = text.isNotBlank() || atts.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(tr("Сохранить")) }
        }
    }

    if (pickDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = toUtcMidnight(time))
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { time = withDate(time, it) }
                    pickDate = false
                }) { Text(tr("Готово")) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(tr("Отмена")) } },
        ) { DatePicker(state) }
    }
    if (pickTime) {
        val cal = Calendar.getInstance().apply { timeInMillis = time }
        val state = rememberTimePickerState(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = {
                    time = Calendar.getInstance().apply {
                        timeInMillis = time
                        set(Calendar.HOUR_OF_DAY, state.hour); set(Calendar.MINUTE, state.minute)
                    }.timeInMillis
                    pickTime = false
                }) { Text(tr("Готово")) }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text(tr("Отмена")) } },
        )
    }
}

/** DatePicker работает с полуночью UTC — переводим локальную дату туда и обратно. */
private fun toUtcMidnight(local: Long): Long {
    val l = Calendar.getInstance().apply { timeInMillis = local }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear(); set(l.get(Calendar.YEAR), l.get(Calendar.MONTH), l.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun withDate(local: Long, utcMidnight: Long): Long {
    val u = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMidnight }
    return Calendar.getInstance().apply {
        timeInMillis = local
        set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private suspend fun buildReport(context: Context, entries: List<JournalEntry>): File {
    val ru = java.util.Locale.getDefault()
    val full = SimpleDateFormat("dd.MM.yyyy HH:mm", ru)
    val recs = Recorder.list(context)
    val docs = Vault.list(context)
    val text = buildString {
        appendLine(tr("ОТЧЁТ О СОБЫТИЯХ"))
        appendLine(tr("Сформирован: %1\$s", full.format(Date())))
        appendLine(tr("Источник: приложение DTM (данные хранились зашифрованно на устройстве)"))
        appendLine()
        appendLine(tr("1. ХРОНОЛОГИЯ СОБЫТИЙ (%1\$s)", entries.size))
        appendLine("-".repeat(40))
        entries.sortedBy { it.time }.forEachIndexed { i, e ->
            appendLine("${i + 1}. ${full.format(Date(e.time))} — ${e.kind}")
            appendLine(e.text)
            if (e.witnesses.isNotBlank()) appendLine(tr("Свидетели: %1\$s", e.witnesses))
            if (e.attachments.isNotEmpty()) appendLine(tr("Вложения: %1\$s (фото/видео/аудио, хранятся в приложении)", e.attachments.size))
            appendLine()
        }
        appendLine(tr("2. АУДИОЗАПИСИ (%1\$s)", recs.size))
        appendLine("-".repeat(40))
        recs.sortedBy { it.startUtc }.forEachIndexed { i, r ->
            val timeKind = if (r.networkTime) tr("сетевое время") else tr("время устройства")
            appendLine(tr("%1\$s. %2\$s (%3\$s), длительность %4\$s, частей: %5\$s", i + 1, full.format(Date(r.startUtc)), timeKind, formatDuration(r.durationSec), r.chunks.size))
            if (r.lat != null && r.lon != null) appendLine(tr("   Координаты: %1\$s, %2\$s", "%.5f".format(r.lat), "%.5f".format(r.lon)))
            r.chunks.forEach { c -> appendLine(tr("   Часть %1\$s (%2\$s) SHA-256: %3\$s", c.part + 1, c.plainName, c.sha256)) }
        }
        appendLine()
        appendLine(tr("3. ДОКУМЕНТЫ И ФОТО (%1\$s)", docs.size))
        appendLine("-".repeat(40))
        docs.sortedBy { it.added }.forEachIndexed { i, d ->
            appendLine(tr("%1\$s. %2\$s — добавлен %3\$s", i + 1, d.name, full.format(Date(d.added))))
        }
        appendLine()
        appendLine(tr("Отпечаток SHA-256 вычислен в момент окончания записи. Совпадение отпечатка у переданного файла подтверждает, что файл не изменялся."))
    }
    val f = File(Crypto.viewDir(context), tr("Отчёт_%1\$s.txt", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())))
    f.writeText(text)
    return f
}
