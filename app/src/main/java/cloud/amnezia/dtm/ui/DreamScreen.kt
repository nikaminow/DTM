@file:OptIn(ExperimentalMaterial3Api::class)

package cloud.amnezia.dtm.ui

import cloud.amnezia.dtm.tr

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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cloud.amnezia.dtm.data.Dream
import cloud.amnezia.dtm.data.Dreams
import kotlinx.coroutines.launch

@Composable
fun DreamScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dreams by remember { mutableStateOf<List<Dream>?>(null) }
    var editing by remember { mutableStateOf<Dream?>(null) }
    var toDelete by remember { mutableStateOf<Dream?>(null) }

    LaunchedEffect(Unit) { dreams = Dreams.load(context) }
    val list = dreams.orEmpty()
    fun persist(l: List<Dream>) { dreams = l.sortedByDescending { it.time }; scope.launch { Dreams.save(context, dreams!!) } }

    ScreenScaffold(
        title = tr("Дневник снов"),
        subtitle = tr("Запишите сон и оцените его. Со временем видно, как меняется ваш сон."),
        onBack = onBack,
    ) {
        item {
            FilledTonalButton(
                onClick = { editing = Dream(System.currentTimeMillis(), System.currentTimeMillis(), "", 2) },
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(56.dp),
            ) { Icon(Icons.Filled.Bedtime, null); Spacer(Modifier.width(8.dp)); Text(tr("Записать сон")) }
        }
        if (dreams != null && list.isEmpty()) item {
            InfoCard(Icons.Filled.Bedtime, tr("Снов пока нет"),
                tr("Записывайте сны сразу после пробуждения — они быстро забываются. Записи шифруются."))
        }
        items(list, key = { it.id }) { d ->
            ItemCard(
                overline = "${ratingEmoji(d.rating)} ${Dreams.word(d.rating)} · ${shortDate(d.time)}",
                headline = d.text.ifBlank { tr("Без описания") },
                supporting = "",
                icon = Icons.Filled.Bedtime,
                onClick = { editing = d },
                actions = listOf(
                    MenuAction(tr("Изменить"), Icons.Filled.Edit) { editing = d },
                    MenuAction(tr("Удалить"), Icons.Filled.Delete, destructive = true) { toDelete = d },
                ),
            )
        }
    }

    editing?.let { d ->
        DreamEditor(d, isNew = list.none { it.id == d.id },
            onSave = { nd -> persist(list.filter { it.id != nd.id } + nd); editing = null },
            onDismiss = { editing = null })
    }
    toDelete?.let { d ->
        ConfirmDialog(tr("Удалить сон?"), tr("Запись будет удалена без возможности восстановления."), tr("Удалить"),
            onConfirm = { persist(list.filter { it.id != d.id }) }, onDismiss = { toDelete = null })
    }
}

private fun ratingEmoji(r: Int) = when (r) { 1 -> "🙁"; 2 -> "😐"; else -> "🤩" }

@Composable
private fun DreamEditor(initial: Dream, isNew: Boolean, onSave: (Dream) -> Unit, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf(initial.text) }
    var rating by remember { mutableIntStateOf(initial.rating) }
    val cs = MaterialTheme.colorScheme

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
                .padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(if (isNew) tr("Новый сон") else tr("Сон"), style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                label = { Text(tr("Что приснилось")) },
                placeholder = { Text(tr("Образы, чувства, что запомнилось…")) },
                minLines = 5, modifier = Modifier.fillMaxWidth(),
            )
            Text(tr("Оцените сон"), style = MaterialTheme.typography.titleSmall, color = cs.primary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RateBox("🙁", tr("Плохой"), rating == 1, cs.error, Modifier.weight(1f)) { rating = 1 }
                RateBox("😐", tr("Средний"), rating == 2, cs.tertiary, Modifier.weight(1f)) { rating = 2 }
                RateBox("🤩", tr("Супер"), rating == 3, cs.primary, Modifier.weight(1f)) { rating = 3 }
            }
            Button(
                onClick = { onSave(initial.copy(text = text.trim(), rating = rating)) },
                enabled = text.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(tr("Сохранить")) }
        }
    }
}

@Composable
private fun RateBox(emoji: String, label: String, selected: Boolean, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) color.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, color) else null,
        modifier = modifier.height(76.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
