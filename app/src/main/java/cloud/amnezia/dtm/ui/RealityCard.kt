@file:OptIn(ExperimentalMaterial3Api::class)

package cloud.amnezia.dtm.ui

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cloud.amnezia.dtm.data.PersonalCard
import kotlinx.coroutines.launch

/** Карточка «Карта реальности» в разделе «Сейчас»: личный текст против чувства вины. */
@Composable
fun RealityCard() {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    var text by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { text = PersonalCard.load(context) }

    val saved = text?.takeIf { it.isNotBlank() }

    Card(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = cs.primaryContainer, contentColor = cs.onPrimaryContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Filled.SelfImprovement, cs.primary, cs.onPrimary)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Карта реальности", style = MaterialTheme.typography.titleMedium)
                    Text("Прочитайте, когда накрывает вина", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(14.dp))
            if (saved != null) {
                Text(saved, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { editing = true }) {
                    Icon(Icons.Filled.Edit, null, Modifier.width(18.dp)); Spacer(Modifier.width(8.dp)); Text("Изменить")
                }
            } else {
                Text(
                    "Составьте текст, который напомнит вам правду в трудный момент: что держать дистанцию — " +
                        "это защита, а не жестокость. Можно начать с готового примера и переписать под себя.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { editing = true }) {
                    Icon(Icons.Filled.AutoAwesome, null, Modifier.width(18.dp)); Spacer(Modifier.width(8.dp)); Text("Создать карту")
                }
            }
        }
    }

    if (editing) {
        RealityCardEditor(
            initial = saved ?: PersonalCard.TEMPLATE,
            onSave = { t -> scope.launch { PersonalCard.save(context, t); text = t; editing = false } },
            onDismiss = { editing = false },
        )
    }
}

@Composable
private fun RealityCardEditor(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var value by remember { mutableStateOf(initial) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
                .padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Карта реальности", style = MaterialTheme.typography.headlineSmall)
            Text("Пишите от первого лица, своими словами. Этот текст видите только вы.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = value, onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(), minLines = 8,
                label = { Text("Текст карты") },
            )
            TextButton(onClick = { value = PersonalCard.TEMPLATE }) { Text("Вернуть пример") }
            Button(
                onClick = { onSave(value.trim()) },
                enabled = value.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Сохранить") }
            Text("Это упражнение из схема-терапии, а не медицинская помощь. Если накрывают тяжёлые мысли — загляните в «Телефоны».",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start)
        }
    }
}
