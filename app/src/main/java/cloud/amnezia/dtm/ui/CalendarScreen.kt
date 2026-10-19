@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package cloud.amnezia.dtm.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cloud.amnezia.dtm.data.CalEvent
import cloud.amnezia.dtm.data.CalendarStore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val RU = Locale("ru")

private fun dayStart(ms: Long) = Calendar.getInstance().apply {
    timeInMillis = ms; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

@Composable
fun CalendarScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    var events by remember { mutableStateOf<List<CalEvent>>(emptyList()) }
    var month by remember { mutableLongStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis) }
    var selected by remember { mutableLongStateOf(dayStart(System.currentTimeMillis())) }
    var editing by remember { mutableStateOf<CalEvent?>(null) }
    var toDelete by remember { mutableStateOf<CalEvent?>(null) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(tick) { events = CalendarStore.load(context) }

    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val daysWith = remember(events) { events.map { dayStart(it.time) }.toSet() }
    val dayEvents = events.filter { dayStart(it.time) == selected }

    ScreenScaffold(title = "Календарь", subtitle = "Свои события и напоминания. Всё хранится зашифрованно на телефоне.", onBack = onBack) {
        item {
            SoftCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HeaderIcon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Назад") {
                        month = Calendar.getInstance().apply { timeInMillis = month; add(Calendar.MONTH, -1) }.timeInMillis
                    }
                    Text(
                        SimpleDateFormat("LLLL yyyy", RU).format(Date(month)).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).padding(start = 12.dp),
                    )
                    HeaderIcon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Вперёд") {
                        month = Calendar.getInstance().apply { timeInMillis = month; add(Calendar.MONTH, 1) }.timeInMillis
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach {
                        Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                val c = Calendar.getInstance().apply { timeInMillis = month }
                val offset = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7
                val count = c.getActualMaximum(Calendar.DAY_OF_MONTH)
                val today = dayStart(System.currentTimeMillis())
                val cells = offset + count
                for (row in 0 until (cells + 6) / 7) {
                    Row(Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val d = row * 7 + col - offset + 1
                            Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                                if (d in 1..count) {
                                    val ms = dayStart(Calendar.getInstance().apply { timeInMillis = month; set(Calendar.DAY_OF_MONTH, d) }.timeInMillis)
                                    val sel = ms == selected
                                    Box(
                                        Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape)
                                            .background(if (sel) cs.primary else if (ms == today) cs.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                                            .softClickable { selected = ms },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("$d", style = MaterialTheme.typography.bodyLarge,
                                            color = if (sel) cs.onPrimary else cs.onSurface)
                                        if (ms in daysWith) Box(
                                            Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).size(5.dp)
                                                .clip(CircleShape).background(if (sel) cs.onPrimary else cs.tertiary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(SimpleDateFormat("d MMMM, EEEE", RU).format(Date(selected)), style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f))
                PillButton("Добавить", {
                    val t = Calendar.getInstance().apply {
                        timeInMillis = selected; set(Calendar.HOUR_OF_DAY, 12)
                    }.timeInMillis
                    editing = CalEvent(System.currentTimeMillis(), t, "", "", 60)
                }, icon = Icons.Filled.Add, style = PillStyle.Soft)
            }
        }
        if (dayEvents.isEmpty()) item {
            Text("В этот день событий нет", Modifier.padding(horizontal = 20.dp), color = cs.onSurfaceVariant)
        }
        items(dayEvents, key = { it.id }) { e ->
            ItemCard(
                overline = SimpleDateFormat("HH:mm", RU).format(Date(e.time)) + " · " + CalendarStore.REMIND.first { it.first == e.remindMin }.second,
                headline = e.title,
                supporting = e.note,
                icon = Icons.Filled.Event,
                onClick = { editing = e },
                actions = listOf(
                    MenuAction("Изменить", Icons.Filled.Edit) { editing = e },
                    MenuAction("Удалить", Icons.Filled.Delete, destructive = true) { toDelete = e },
                ),
            )
        }
    }

    editing?.let { e ->
        EventEditor(e, onDismiss = { editing = null }) { ne ->
            editing = null
            if (ne.remindMin >= 0 && Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
            scope.launch { CalendarStore.upsert(context, ne); tick++ }
        }
    }
    toDelete?.let { e ->
        ConfirmDialog("Удалить событие?", "«${e.title}» будет удалено вместе с напоминанием.", "Удалить",
            onConfirm = { scope.launch { CalendarStore.delete(context, e); tick++ } }, onDismiss = { toDelete = null })
    }
}

@Composable
private fun EventEditor(initial: CalEvent, onDismiss: () -> Unit, onSave: (CalEvent) -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(initial.title) }
    var note by remember { mutableStateOf(initial.note) }
    var time by remember { mutableLongStateOf(initial.time) }
    var remind by remember { mutableIntStateOf(initial.remindMin) }
    var pickTime by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
                .padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(SimpleDateFormat("d MMMM yyyy", RU).format(Date(time)), style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(title, { title = it }, label = { Text("Что запланировано") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(note, { note = it }, label = { Text("Заметка") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            PillButton("Время: " + SimpleDateFormat("HH:mm", RU).format(Date(time)), { pickTime = true },
                Modifier.fillMaxWidth(), Icons.Filled.Schedule, PillStyle.Ghost)
            Text("Напоминание", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CalendarStore.REMIND.forEach { (m, label) -> SoftChip(label, remind == m) { remind = m } }
            }
            PillButton("Сохранить", { onSave(initial.copy(title = title.trim(), note = note.trim(), time = time, remindMin = remind)) },
                Modifier.fillMaxWidth(), enabled = title.isNotBlank())
        }
    }

    if (pickTime) {
        val c = Calendar.getInstance().apply { timeInMillis = time }
        val st = rememberTimePickerState(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            text = { TimePicker(st) },
            confirmButton = {
                TextButton(onClick = {
                    time = Calendar.getInstance().apply {
                        timeInMillis = time; set(Calendar.HOUR_OF_DAY, st.hour); set(Calendar.MINUTE, st.minute); set(Calendar.SECOND, 0)
                    }.timeInMillis
                    pickTime = false
                }) { Text("Готово") }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text("Отмена") } },
        )
    }
}
