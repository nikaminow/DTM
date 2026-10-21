@file:OptIn(ExperimentalLayoutApi::class)

package cloud.amnezia.dtm.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cloud.amnezia.dtm.data.Dbt
import cloud.amnezia.dtm.data.DbtLesson
import cloud.amnezia.dtm.data.LESSONS
import cloud.amnezia.dtm.tr

private data class Situation(val label: String, val hint: String, val lessons: List<String>)

private val SITUATIONS get() = listOf(
    Situation(tr("Эмоция захлёстывает"), tr("Сначала снизить накал через тело, потом решать."), listOf("tipp", "stop", "54321")),
    Situation(tr("Мысли крутятся по кругу"), tr("Вернуться в настоящее и отделить мысли от фактов."), listOf("what", "check", "wise")),
    Situation(tr("Тревога, паника"), tr("Опора на органы чувств и дыхание."), listOf("54321", "tipp")),
    Situation(tr("Хочется сделать что-то сгоряча"), tr("Пауза и взвешивание последствий."), listOf("stop", "prosandcons")),
    Situation(tr("Не могу ничего изменить"), tr("Принятие того, что есть, и выбор следующего шага."), listOf("radical", "wise")),
    Situation(tr("Не понимаю, что чувствую"), tr("Назвать эмоцию и проверить факты."), listOf("name", "check")),
    Situation(tr("Нужно попросить или отказать"), tr("Ясная просьба с уважением к себе и другому."), listOf("dearman", "fast", "give")),
    Situation(tr("Нет сил, всё серо"), tr("Забота о теле и маленький шаг к важному."), listOf("please", "values", "opposite")),
)

@Composable
fun SkillFinder(open: (DbtLesson) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val list = remember { SITUATIONS }
    var sel by remember { mutableIntStateOf(-1) }
    SoftCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
        Text(tr("Что происходит сейчас?"), style = MaterialTheme.typography.titleLarge)
        Text(tr("Выберите — и мы подскажем подходящий навык."), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            list.forEachIndexed { i, s -> SoftChip(s.label, sel == i) { sel = if (sel == i) -1 else i } }
        }
        AnimatedVisibility(sel >= 0, enter = fadeIn() + expandVertically()) {
            val s = list.getOrNull(sel)
            if (s != null) Column(Modifier.padding(top = 14.dp)) {
                Text(s.hint, style = MaterialTheme.typography.bodyMedium, color = cs.primary)
                Spacer(Modifier.height(8.dp))
                s.lessons.mapNotNull { id -> Dbt.LESSONS.firstOrNull { it.id == id } }.forEach { l ->
                    Row(
                        Modifier.fillMaxWidth().softClickable { open(l) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(l.title, style = MaterialTheme.typography.titleSmall)
                            Text(l.summary, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = cs.primary)
                    }
                }
            }
        }
    }
}
