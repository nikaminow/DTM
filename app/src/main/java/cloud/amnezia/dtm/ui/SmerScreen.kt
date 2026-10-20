@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package cloud.amnezia.dtm.ui

import cloud.amnezia.dtm.tr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cloud.amnezia.dtm.data.Smer
import cloud.amnezia.dtm.data.SmerEntry
import kotlinx.coroutines.launch

@Composable
fun SmerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<SmerEntry>?>(null) }
    var editing by remember { mutableStateOf<SmerEntry?>(null) }
    var toDelete by remember { mutableStateOf<SmerEntry?>(null) }

    LaunchedEffect(Unit) { entries = Smer.load(context) }
    val list = entries.orEmpty()
    fun persist(l: List<SmerEntry>) {
        val sorted = l.sortedByDescending { it.time }
        entries = sorted
        scope.launch { Smer.save(context, sorted) }
    }

    ScreenScaffold(
        title = tr("Дневник СМЭР"),
        subtitle = tr("Ситуация → Мысли → Эмоции → Реакции тела и поведения. Помогает увидеть, как мысль запускает чувство и действие."),
        onBack = onBack,
    ) {
        item {
            FilledTonalButton(
                onClick = { editing = SmerEntry(System.currentTimeMillis(), System.currentTimeMillis(), "", "", emptyList(), "", "") },
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(56.dp),
            ) { Icon(Icons.Filled.Psychology, null); Spacer(Modifier.width(8.dp)); Text(tr("Новая запись")) }
        }
        if (entries != null && list.isEmpty()) item {
            InfoCard(Icons.Filled.Psychology, tr("Пример"),
                tr("Ситуация: стою в пробке в автобусе.\n") +
                    tr("Мысли: «Если ещё постоим — я запаникую, все заметят и решат, что я псих».\n") +
                    tr("Эмоции: тревога.\n") +
                    tr("Тело: жар в груди, тремор, сердцебиение.\n") +
                    tr("Поведение: звоню брату, отвлекаюсь разговором."))
        }
        items(list, key = { it.id }) { e ->
            ItemCard(
                overline = (e.emotions.joinToString(", ").ifBlank { tr("Эмоции не указаны") }) + " · " + shortDate(e.time),
                headline = e.situation.ifBlank { tr("Без описания ситуации") },
                supporting = if (e.thoughts.isNotBlank()) tr("Мысли: %1\$s", e.thoughts) else "",
                icon = Icons.Filled.Psychology,
                onClick = { editing = e },
                actions = listOf(
                    MenuAction(tr("Изменить"), Icons.Filled.Edit) { editing = e },
                    MenuAction(tr("Удалить"), Icons.Filled.Delete, destructive = true) { toDelete = e },
                ),
            )
        }
    }

    editing?.let { e ->
        SmerEditor(e, isNew = list.none { it.id == e.id },
            onSave = { ne -> persist(list.filter { it.id != ne.id } + ne); editing = null },
            onDismiss = { editing = null })
    }
    toDelete?.let { e ->
        ConfirmDialog(tr("Удалить запись?"), tr("Запись будет удалена без возможности восстановления."), tr("Удалить"),
            onConfirm = { persist(list.filter { it.id != e.id }) }, onDismiss = { toDelete = null })
    }
}

@Composable
private fun SmerEditor(initial: SmerEntry, isNew: Boolean, onSave: (SmerEntry) -> Unit, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var situation by remember { mutableStateOf(initial.situation) }
    var thoughts by remember { mutableStateOf(initial.thoughts) }
    val emotions = remember { mutableStateListOf<String>().apply { addAll(initial.emotions) } }
    var customEmotion by remember { mutableStateOf("") }
    var body by remember { mutableStateOf(initial.body) }
    var behavior by remember { mutableStateOf(initial.behavior) }
    val cs = MaterialTheme.colorScheme

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
                .padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (isNew) tr("Новая запись СМЭР") else tr("Запись СМЭР"), style = MaterialTheme.typography.headlineSmall)

            Text(tr("С — Ситуация"), style = MaterialTheme.typography.titleSmall, color = cs.primary)
            OutlinedTextField(situation, { situation = it }, placeholder = { Text(tr("Что произошло, где, когда")) },
                minLines = 2, modifier = Modifier.fillMaxWidth())

            Text(tr("М — Мысли"), style = MaterialTheme.typography.titleSmall, color = cs.primary)
            OutlinedTextField(thoughts, { thoughts = it }, placeholder = { Text(tr("Что пронеслось в голове? Запишите дословно")) },
                minLines = 3, modifier = Modifier.fillMaxWidth())

            Text(tr("Э — Эмоции"), style = MaterialTheme.typography.titleSmall, color = cs.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (Smer.EMOTIONS + emotions.filter { it !in Smer.EMOTIONS }).forEach { em ->
                    FilterChip(
                        selected = em in emotions,
                        onClick = { if (em in emotions) emotions.remove(em) else emotions.add(em) },
                        label = { Text(em) },
                    )
                }
            }
            OutlinedTextField(
                customEmotion, { customEmotion = it },
                placeholder = { Text(tr("Другая эмоция")) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    androidx.compose.material3.TextButton(
                        onClick = { val t = customEmotion.trim(); if (t.isNotEmpty() && t !in emotions) emotions.add(t); customEmotion = "" },
                        enabled = customEmotion.isNotBlank(),
                    ) { Text(tr("Добавить")) }
                },
            )

            Text(tr("Р — Реакции"), style = MaterialTheme.typography.titleSmall, color = cs.primary)
            OutlinedTextField(body, { body = it }, label = { Text(tr("Тело")) },
                placeholder = { Text(tr("Жар, дрожь, напряжение, сердцебиение…")) }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(behavior, { behavior = it }, label = { Text(tr("Поведение")) },
                placeholder = { Text(tr("Что вы сделали или захотелось сделать")) }, minLines = 2, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    onSave(initial.copy(situation = situation.trim(), thoughts = thoughts.trim(), emotions = emotions.toList(),
                        body = body.trim(), behavior = behavior.trim()))
                },
                enabled = situation.isNotBlank() || thoughts.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(tr("Сохранить")) }
        }
    }
}
