package cloud.amnezia.dtm.ui

import cloud.amnezia.dtm.tr

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cloud.amnezia.dtm.data.LogEntry
import cloud.amnezia.dtm.data.MoodEntry
import cloud.amnezia.dtm.data.Tracker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ProgressScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var moods by remember { mutableStateOf<List<MoodEntry>>(emptyList()) }
    var logs by remember { mutableStateOf<List<LogEntry>>(emptyList()) }
    LaunchedEffect(Unit) {
        moods = Tracker.moods(context)
        logs = Tracker.logs(context)
    }

    ScreenScaffold(
        title = tr("Прогресс"),
        subtitle = tr("Как менялось ваше состояние и что вы делали для себя."),
        onBack = onBack,
    ) {
        item { MoodChartCard(moods) }
        item { MoodStats(moods) }

        if (logs.isNotEmpty()) {
            item { SectionLabel(tr("Дневник практик · %1\$s", logs.size)) }
            items(logs, key = { it.time }) { e -> LogRow(e) }
        } else {
            item {
                InfoCard(Icons.Filled.Check, tr("Здесь будут ваши практики"),
                    tr("Отметки состояния, упражнения и техники, которые вы выполняете, автоматически попадают сюда."))
            }
        }
    }
}

@Composable
private fun MoodChartCard(moods: List<MoodEntry>) {
    val cs = MaterialTheme.colorScheme
    val days = 14
    val cal = Calendar.getInstance()
    val todayStart = cal.apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val dayMs = 24L * 3600 * 1000
    // Значение настроения по каждому из последних 14 дней (0 = нет отметки).
    // Среднее за каждый день (отметок в день может быть несколько).
    val sum = IntArray(days); val cnt = IntArray(days)
    moods.forEach { m ->
        val start = todayStart - (days - 1) * dayMs
        if (m.time >= start) {
            val idx = ((m.time - start) / dayMs).toInt()
            if (idx in 0 until days) { sum[idx] += m.value; cnt[idx]++ }
        }
    }
    val avgDay = FloatArray(days) { if (cnt[it] > 0) sum[it].toFloat() / cnt[it] else 0f }
    val dayFmt = SimpleDateFormat("d", java.util.Locale.getDefault())

    Card(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(tr("Состояние за 2 недели"), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                for (i in 0 until days) {
                    val avg = avgDay[i]
                    val color = when {
                        avg == 0f -> cs.surfaceContainerHighest
                        avg < 1.67f -> cs.error
                        avg < 2.34f -> cs.tertiary
                        else -> cs.primary
                    }
                    val frac = if (avg == 0f) 0.06f else avg / 3f
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.fillMaxWidth().height((110 * frac).dp)
                                .clip(RoundedCornerShape(6.dp)).background(color)
                        )
                        Spacer(Modifier.height(4.dp))
                        val t = todayStart - (days - 1 - i) * dayMs
                        Text(dayFmt.format(Date(t)), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Legend(cs.error, tr("Плохо")); Legend(cs.tertiary, tr("Средне")); Legend(cs.primary, tr("Отлично"))
            }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MoodStats(moods: List<MoodEntry>) {
    val cs = MaterialTheme.colorScheme
    val last30 = moods.filter { it.time > System.currentTimeMillis() - 30L * 24 * 3600 * 1000 }
    val avg = if (last30.isNotEmpty()) last30.map { it.value }.average() else 0.0
    val streak = calcStreak(moods)
    Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatBox(if (avg > 0) "%.1f".format(avg) else "—", tr("среднее за 30 дней"), Modifier.weight(1f))
        StatBox("$streak", tr("дней подряд с отметкой"), Modifier.weight(1f))
    }
}

@Composable
private fun StatBox(value: String, label: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LogRow(e: LogEntry) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.title, style = MaterialTheme.typography.titleSmall)
                if (e.detail.isNotBlank()) Text(e.detail, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(10.dp))
            Text(shortDate(e.time), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
        }
    }
}

private fun calcStreak(moods: List<MoodEntry>): Int {
    if (moods.isEmpty()) return 0
    val days = moods.map { dayIndex(it.time) }.toSortedSet().toList()
    val today = dayIndex(System.currentTimeMillis())
    // Серия учитывается, если последняя отметка сегодня или вчера.
    var cursor = when (days.last()) { today -> today; today - 1 -> today - 1; else -> return 0 }
    var streak = 0
    val set = days.toHashSet()
    while (set.contains(cursor)) { streak++; cursor-- }
    return streak
}

private fun dayIndex(ms: Long): Long = ms / (24L * 3600 * 1000)
