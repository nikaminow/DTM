package cloud.amnezia.dtm.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cloud.amnezia.dtm.data.Dbt
import cloud.amnezia.dtm.data.DbtLesson
import cloud.amnezia.dtm.data.LESSONS
import cloud.amnezia.dtm.tr

fun dbtLevel(done: Int, total: Int): String {
    val p = if (total == 0) 0f else done.toFloat() / total
    return when {
        done == 0 -> tr("Начало пути")
        p < 0.3f -> tr("Новичок")
        p < 0.7f -> tr("Практик")
        p < 1f -> tr("Уверенный практик")
        else -> tr("Мастер навыков")
    }
}

@Composable
fun DbtHero(tick: Int, onContinue: (DbtLesson) -> Unit, onDiary: () -> Unit, onCharts: () -> Unit) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val lessons = remember { Dbt.LESSONS }
    val done = remember(tick) { Dbt.done(context) }
    val total = lessons.size
    val next = lessons.firstOrNull { it.id !in done }
    var skillsUsed by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var diaryToday by remember { mutableStateOf(false) }
    val ring = remember { Animatable(0f) }

    LaunchedEffect(tick) {
        val diary = Dbt.diary(context)
        val dayMs = 86_400_000L
        val now = System.currentTimeMillis()
        skillsUsed = diary.filter { now - it.day < 30 * dayMs }.flatMap { it.skills }.toSet().size
        val days = diary.map { it.day / dayMs }.toHashSet()
        var cur = now / dayMs
        if (!days.contains(cur)) cur -= 1
        var s = 0
        while (days.contains(cur)) { s++; cur-- }
        streak = s
        diaryToday = days.contains(now / dayMs)
        ring.animateTo(if (total == 0) 0f else done.size.toFloat() / total, tween(900, easing = FastOutSlowInEasing))
    }

    val shape = RoundedCornerShape(32.dp)
    Column(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(shape)
            .background(Brush.linearGradient(listOf(cs.primaryContainer, cs.secondaryContainer)))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                val track = cs.onPrimaryContainer.copy(alpha = 0.15f)
                val fg = cs.primary
                Canvas(Modifier.size(92.dp)) {
                    val sw = 9.dp.toPx()
                    val tl = Offset(sw / 2, sw / 2)
                    val sz = Size(size.width - sw, size.height - sw)
                    drawArc(track, -90f, 360f, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                    drawArc(fg, -90f, 360f * ring.value, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(ring.value * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, color = cs.onPrimaryContainer)
                    Text("${done.size}/$total", style = MaterialTheme.typography.labelSmall, color = cs.onPrimaryContainer.copy(alpha = 0.7f))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Навыки ДПТ"), style = MaterialTheme.typography.labelLarge, color = cs.primary)
                Text(dbtLevel(done.size, total), style = MaterialTheme.typography.headlineSmall, color = cs.onPrimaryContainer)
                Text(tr("Уроков пройдено: %1\$s из %2\$s", done.size, total), style = MaterialTheme.typography.bodyMedium,
                    color = cs.onPrimaryContainer.copy(alpha = 0.8f))
            }
        }

        Spacer(Modifier.height(16.dp))
        Dbt.MODULES.forEach { m ->
            val (d, t) = Dbt.progress(context, m.id)
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(m.title, style = MaterialTheme.typography.labelMedium, color = cs.onPrimaryContainer,
                    modifier = Modifier.width(140.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(cs.onPrimaryContainer.copy(alpha = 0.12f))) {
                    Box(Modifier.fillMaxWidth(if (t == 0) 0f else d.toFloat() / t).height(8.dp).clip(RoundedCornerShape(4.dp)).background(cs.primary))
                }
                Text(" $d/$t", style = MaterialTheme.typography.labelSmall, color = cs.onPrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.width(36.dp))
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroStat("$skillsUsed/${Dbt.SKILLS.size}", tr("навыков применено за 30 дней"), Modifier.weight(1f))
            HeroStat("$streak", tr("дней подряд в дневнике"), Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))
        if (next != null) {
            PillButton(tr("Продолжить: %1\$s", next.title), { onContinue(next) }, Modifier.fillMaxWidth(),
                icon = Icons.AutoMirrored.Filled.ArrowForward)
        } else {
            Text(tr("Все уроки пройдены. Повторяйте навыки и ведите дневник."), style = MaterialTheme.typography.bodyMedium,
                color = cs.onPrimaryContainer)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(if (diaryToday) tr("Дневник ✓") else tr("Дневник дня"), onDiary, Modifier.weight(1f),
                icon = Icons.Filled.EditNote, style = PillStyle.Ghost)
            PillButton(tr("Графики"), onCharts, Modifier.weight(1f), icon = Icons.Filled.Insights, style = PillStyle.Ghost)
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(cs.surface.copy(alpha = 0.55f)).padding(12.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = cs.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, maxLines = 2)
    }
}
