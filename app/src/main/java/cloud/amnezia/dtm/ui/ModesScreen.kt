@file:OptIn(ExperimentalMaterial3Api::class)

package cloud.amnezia.dtm.ui

import cloud.amnezia.dtm.tr

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cloud.amnezia.dtm.data.ModeEntry
import cloud.amnezia.dtm.data.Modes
import kotlinx.coroutines.launch

@Composable
fun ModesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<ModeEntry>?>(null) }
    var editing by remember { mutableStateOf<ModeEntry?>(null) }
    var toDelete by remember { mutableStateOf<ModeEntry?>(null) }
    var showHelp by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { entries = Modes.load(context) }
    val list = entries.orEmpty()
    fun persist(l: List<ModeEntry>) { entries = l.sortedByDescending { it.time }; scope.launch { Modes.save(context, entries!!) } }

    ScreenScaffold(
        title = tr("Дневник режимов"),
        subtitle = tr("Какой режим включился, что его запустило и что ответил бы Здоровый Взрослый. Из схема-терапии."),
        onBack = onBack,
        actions = {
            androidx.compose.material3.IconButton(onClick = { showHelp = true }) { Icon(Icons.Outlined.Info, tr("О режимах")) }
        },
    ) {
        item {
            FilledTonalButton(
                onClick = { editing = ModeEntry(System.currentTimeMillis(), System.currentTimeMillis(), emptyList(), "", "", "") },
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(56.dp),
            ) { Icon(Icons.Filled.Groups, null); Spacer(Modifier.width(8.dp)); Text(tr("Отметить режим")) }
        }
        if (entries != null && list.isEmpty()) item {
            InfoCard(Icons.Filled.Groups, tr("Записей пока нет"),
                tr("Замечайте, когда внутри включается «режим» — например, Карающий Родитель или Отстранённый Защитник. ") +
                    tr("Называя режим, легче вернуть управление Здоровому Взрослому. Нажмите ⓘ вверху для описаний."))
        }
        items(list, key = { it.id }) { e ->
            ItemCard(
                overline = e.modes.joinToString(" · ").ifBlank { tr("Режим не выбран") } + " · " + shortDate(e.time),
                headline = e.trigger.ifBlank { tr("Без описания ситуации") },
                supporting = if (e.healthyAdult.isNotBlank()) tr("Здоровый Взрослый: %1\$s", e.healthyAdult) else "",
                icon = Icons.Filled.Groups,
                onClick = { editing = e },
                actions = listOf(
                    MenuAction(tr("Изменить"), Icons.Filled.Edit) { editing = e },
                    MenuAction(tr("Удалить"), Icons.Filled.Delete, destructive = true) { toDelete = e },
                ),
            )
        }
    }

    editing?.let { e ->
        ModeEditor(e, isNew = list.none { it.id == e.id },
            onSave = { ne -> persist(list.filter { it.id != ne.id } + ne); editing = null },
            onDismiss = { editing = null })
    }
    toDelete?.let { e ->
        ConfirmDialog(tr("Удалить запись?"), tr("Запись будет удалена без возможности восстановления."), tr("Удалить"),
            onConfirm = { persist(list.filter { it.id != e.id }) }, onDismiss = { toDelete = null })
    }
    if (showHelp) ModesHelp { showHelp = false }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModeEditor(initial: ModeEntry, isNew: Boolean, onSave: (ModeEntry) -> Unit, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selected = remember { androidx.compose.runtime.mutableStateListOf<String>().apply { addAll(initial.modes) } }
    var trigger by remember { mutableStateOf(initial.trigger) }
    var voice by remember { mutableStateOf(initial.voice) }
    var ha by remember { mutableStateOf(initial.healthyAdult) }
    val cs = MaterialTheme.colorScheme

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
                .padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (isNew) tr("Какой режим включился?") else tr("Режим"), style = MaterialTheme.typography.headlineSmall)
            Modes.GROUPS.forEach { group ->
                Text(group, style = MaterialTheme.typography.titleSmall, color = cs.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Modes.CATALOG.filter { it.group == group }.forEach { m ->
                        FilterChip(
                            selected = m.name in selected,
                            onClick = { if (m.name in selected) selected.remove(m.name) else selected.add(m.name) },
                            label = { Text(m.name) },
                        )
                    }
                }
            }
            OutlinedTextField(trigger, { trigger = it }, label = { Text(tr("Что запустило режим")) },
                placeholder = { Text(tr("Ситуация, слова, событие")) }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(voice, { voice = it }, label = { Text(tr("Что он говорит или делает")) },
                placeholder = { Text(tr("Мысли, чувства, побуждения этого режима")) }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ha, { ha = it }, label = { Text(tr("Ответ Здорового Взрослого")) },
                placeholder = { Text(tr("Что сказал бы заботливый и мудрый взрослый?")) }, minLines = 2, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = { onSave(initial.copy(modes = selected.toList(), trigger = trigger.trim(), voice = voice.trim(), healthyAdult = ha.trim())) },
                enabled = selected.isNotEmpty() || trigger.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(tr("Сохранить")) }
        }
    }
}

@Composable
private fun ModesHelp(onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(tr("Режимы"), style = MaterialTheme.typography.headlineSmall)
            Modes.GROUPS.forEach { group ->
                Text(group, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Modes.CATALOG.filter { it.group == group }.forEach { m ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().animateContentSize()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(m.name, style = MaterialTheme.typography.titleSmall)
                            Text(m.about, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Text(tr("Это обучающий материал, а не диагностика. Разбирать режимы глубже лучше со специалистом по схема-терапии."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
