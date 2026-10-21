@file:OptIn(ExperimentalLayoutApi::class)

package cloud.amnezia.dtm.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cloud.amnezia.dtm.data.Dbt
import cloud.amnezia.dtm.data.DbtEntry
import cloud.amnezia.dtm.data.DbtLesson
import cloud.amnezia.dtm.data.DiaryDay
import cloud.amnezia.dtm.data.LESSONS
import cloud.amnezia.dtm.tr
import kotlinx.coroutines.launch
import java.util.Calendar

private sealed class DbtView {
    object Home : DbtView()
    data class Module(val id: Int) : DbtView()
    data class Lesson(val lesson: DbtLesson) : DbtView()
    object Diary : DbtView()
    object Chart : DbtView()
}

@Composable
fun DbtScreen(onBack: () -> Unit) {
    var view by remember { mutableStateOf<DbtView>(DbtView.Home) }
    var tick by remember { mutableIntStateOf(0) }
    androidx.activity.compose.BackHandler(view !is DbtView.Home) {
        view = if (view is DbtView.Lesson) DbtView.Module((view as DbtView.Lesson).lesson.module) else DbtView.Home
    }
    AnimatedContent(view, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) }, label = "dbt") { v ->
        when (v) {
            is DbtView.Home -> DbtHome(tick, onBack,
                openModule = { view = DbtView.Module(it) }, openDiary = { view = DbtView.Diary }, openChart = { view = DbtView.Chart })
            is DbtView.Module -> DbtModuleView(v.id, onBack = { view = DbtView.Home }, open = { view = DbtView.Lesson(it) })
            is DbtView.Lesson -> DbtLessonView(v.lesson, onBack = { view = DbtView.Module(v.lesson.module) }, onDone = { tick++; view = DbtView.Module(v.lesson.module) })
            is DbtView.Diary -> DbtDiaryView(onBack = { view = DbtView.Home })
            is DbtView.Chart -> DbtChartView(onBack = { view = DbtView.Home })
        }
    }
}

@Composable
private fun DbtHome(tick: Int, onBack: () -> Unit, openModule: (Int) -> Unit, openDiary: () -> Unit, openChart: () -> Unit) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val done = remember(tick) { Dbt.done(context) }
    val total = Dbt.LESSONS.size
    ScreenScaffold(
        title = tr("Курс ДПТ"),
        subtitle = tr("Самостоятельный курс навыков: осознанность, стрессоустойчивость, эмоции и отношения. Проходите в своём темпе."),
        onBack = onBack,
    ) {
        item {
            SoftCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), color = cs.primaryContainer, contentColor = cs.onPrimaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ring(done.size.toFloat() / total, 64.dp, cs.onPrimaryContainer, cs.primary) {
                        Text("${done.size}/$total", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr("Ваш прогресс"), style = MaterialTheme.typography.titleLarge)
                        Text(tr("Пройдено уроков: %1\$s из %2\$s", done.size, total), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PillButton(tr("Дневник навыков"), openDiary, Modifier.weight(1f), style = PillStyle.Soft)
                PillButton(tr("Графики"), openChart, Modifier.weight(1f), style = PillStyle.Soft)
            }
        }
        items(Dbt.MODULES, key = { it.id }) { m ->
            val (d, t) = Dbt.progress(context, m.id)
            SoftCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), onClick = { openModule(m.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ring(if (t == 0) 0f else d.toFloat() / t, 46.dp, cs.primary, cs.surfaceContainerHighest) {
                        if (d == t && t > 0) Icon(Icons.Filled.Check, null, tint = cs.primary, modifier = Modifier.size(20.dp))
                        else Text("$d/$t", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${m.id}. ${m.title}", style = MaterialTheme.typography.titleMedium)
                        Text(m.about, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun DbtModuleView(id: Int, onBack: () -> Unit, open: (DbtLesson) -> Unit) {
    val context = LocalContext.current
    val m = Dbt.module(id)
    val lessons = Dbt.lessonsOf(id)
    ScreenScaffold(title = m.title, subtitle = m.about, onBack = onBack) {
        items(lessons, key = { it.id }) { l ->
            ItemCard(
                overline = if (Dbt.isDone(context, l.id)) tr("Пройдено") else tr("Урок"),
                headline = l.title, supporting = l.summary,
                icon = if (Dbt.isDone(context, l.id)) Icons.Filled.Check else Icons.Filled.MenuBook,
                onClick = { open(l) }, actions = listOf(MenuAction(tr("Открыть"), Icons.Filled.MenuBook) { open(l) }),
            )
        }
    }
}

@Composable
private fun DbtLessonView(l: DbtLesson, onBack: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val answers = remember(l.id) { mutableStateListOf(*Array(l.prompts.size) { "" }) }
    var before by remember(l.id) { mutableIntStateOf(5) }
    var after by remember(l.id) { mutableIntStateOf(5) }

    ScreenScaffold(title = l.title, subtitle = l.summary, onBack = onBack, bottomSpace = false) {
        item { SectionLabel(tr("Теория")) }
        items(l.theory) { p ->
            SoftCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                Text(p, style = MaterialTheme.typography.bodyLarge)
            }
        }
        item { SectionLabel(tr("Практика")) }
        item {
            SoftCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                Text(l.practiceIntro, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                if (l.scale) {
                    Spacer(Modifier.height(10.dp))
                    ScaleRow(tr("Напряжение до практики"), before) { before = it }
                }
            }
        }
        l.prompts.forEachIndexed { i, q ->
            item {
                SoftCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                    Text(q, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(answers[i], { answers[i] = it }, minLines = 2, modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(tr("Ваш ответ")) })
                }
            }
        }
        if (l.scale) item {
            SoftCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                ScaleRow(tr("Напряжение после практики"), after) { after = it }
            }
        }
        item {
            PillButton(tr("Отметить пройденным"), {
                scope.launch {
                    Dbt.addPractice(context, DbtEntry(l.id, System.currentTimeMillis(), answers.toList(), if (l.scale) before else -1, if (l.scale) after else -1))
                    onDone()
                }
            }, Modifier.padding(horizontal = 16.dp).fillMaxWidth())
        }
    }
}

@Composable
private fun ScaleRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Column {
        Row {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("$value/10", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value.toFloat(), { onChange(it.toInt()) }, valueRange = 0f..10f, steps = 9)
    }
}

@Composable
private fun Ring(progress: Float, size: androidx.compose.ui.unit.Dp, fg: Color, track: Color, center: @Composable () -> Unit) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().size(size)) {
            val sw = 6.dp.toPx()
            drawArc(track, -90f, 360f, false, style = Stroke(sw, cap = StrokeCap.Round),
                topLeft = Offset(sw / 2, sw / 2), size = Size(this.size.width - sw, this.size.height - sw))
            drawArc(fg, -90f, 360f * progress.coerceIn(0f, 1f), false, style = Stroke(sw, cap = StrokeCap.Round),
                topLeft = Offset(sw / 2, sw / 2), size = Size(this.size.width - sw, this.size.height - sw))
        }
        center()
    }
}

// ---- Дневник навыков ----

@Composable
private fun DbtDiaryView(onBack: () -> Unit) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<DiaryDay>>(emptyList()) }
    val today = remember { todayStart() }
    val emotions = remember { mutableStateMapOf<String, Int>().apply { Dbt.DIARY_EMOTIONS.forEach { put(it, 0) } } }
    var urge by remember { mutableIntStateOf(0) }
    val skills = remember { mutableStateListOf<String>() }
    var note by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        list = Dbt.diary(context)
        Dbt.diaryFor(list, today)?.let { d ->
            d.emotions.forEach { (k, v) -> emotions[k] = v }
            urge = d.urge; skills.clear(); skills.addAll(d.skills); note = d.note
        }
    }

    ScreenScaffold(
        title = tr("Дневник навыков"),
        subtitle = tr("Отмечайте раз в день: эмоции, побуждения и какие навыки применяли. Со временем виден прогресс."),
        onBack = onBack, bottomSpace = false,
    ) {
        item {
            SoftCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                Text(tr("Сила эмоций сегодня"), style = MaterialTheme.typography.titleMedium)
                Dbt.DIARY_EMOTIONS.forEach { e ->
                    Spacer(Modifier.height(4.dp))
                    Row {
                        Text(e, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text("${emotions[e]}/5", style = MaterialTheme.typography.labelLarge, color = cs.primary)
                    }
                    Slider((emotions[e] ?: 0).toFloat(), { emotions[e] = it.toInt() }, valueRange = 0f..5f, steps = 4)
                }
            }
        }
        item {
            SoftCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                Row {
                    Text(tr("Сила опасных побуждений"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text("$urge/5", style = MaterialTheme.typography.labelLarge, color = cs.primary)
                }
                Slider(urge.toFloat(), { urge = it.toInt() }, valueRange = 0f..5f, steps = 4)
            }
        }
        item {
            SoftCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                Text(tr("Какие навыки применяли"), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Dbt.SKILLS.forEach { sk ->
                        SoftChip(sk, sk in skills) { if (sk in skills) skills.remove(sk) else skills.add(sk) }
                    }
                }
            }
        }
        item {
            SoftCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                OutlinedTextField(note, { note = it }, minLines = 2, modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr("Заметка о дне")) })
            }
        }
        item {
            Column {
                PillButton(tr("Сохранить день"), {
                    scope.launch {
                        Dbt.saveDiary(context, DiaryDay(today, emotions.toMap(), urge, skills.toList(), note.trim()))
                        list = Dbt.diary(context); saved = true
                    }
                }, Modifier.padding(horizontal = 16.dp).fillMaxWidth())
                if (saved) Text(tr("Сохранено"), color = cs.primary, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

// ---- Графики ----

@Composable
private fun DbtChartView(onBack: () -> Unit) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    var diary by remember { mutableStateOf<List<DiaryDay>>(emptyList()) }
    var practice by remember { mutableStateOf<List<DbtEntry>>(emptyList()) }
    LaunchedEffect(Unit) { diary = Dbt.diary(context); practice = Dbt.practice(context) }

    ScreenScaffold(title = tr("Графики"), subtitle = tr("Как меняются эмоции и помогает ли практика."), onBack = onBack) {
        item {
            SoftCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                Text(tr("Средняя сила эмоций за 14 дней"), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                DiaryChart(diary, cs)
            }
        }
        item {
            SoftCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                Text(tr("Побуждения за 14 дней"), style = MaterialTheme.typography.titleMedium)
                Text(tr("Чем ниже столбики, тем реже тянуло к опасным действиям."), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                UrgeChart(diary, cs)
            }
        }
        item {
            val withScale = practice.filter { it.before >= 0 }
            val avg = if (withScale.isEmpty()) 0.0 else withScale.map { (it.before - it.after).toDouble() }.average()
            SoftCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                Text(tr("Эффект практики"), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(if (withScale.isEmpty()) tr("Пройдите уроки со шкалой, чтобы увидеть эффект.")
                    else tr("В среднем напряжение снижается на %1\$s балла за практику (упражнений: %2\$s).", "%.1f".format(avg), withScale.size),
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun DiaryChart(diary: List<DiaryDay>, cs: androidx.compose.material3.ColorScheme) {
    val days = 14
    val start = todayStart() - (days - 1) * 86_400_000L
    val vals = FloatArray(days)
    diary.forEach { d ->
        val idx = ((d.day - start) / 86_400_000L).toInt()
        if (idx in 0 until days && d.emotions.isNotEmpty()) {
            val neg = listOf("Грусть", "Тревога", "Гнев", "Стыд").mapNotNull { d.emotions[it] }
            if (neg.isNotEmpty()) vals[idx] = neg.average().toFloat()
        }
    }
    Bars(vals, 5f, cs.tertiary, cs.surfaceContainerHighest)
}

@Composable
private fun UrgeChart(diary: List<DiaryDay>, cs: androidx.compose.material3.ColorScheme) {
    val days = 14
    val start = todayStart() - (days - 1) * 86_400_000L
    val vals = FloatArray(days)
    diary.forEach { d ->
        val idx = ((d.day - start) / 86_400_000L).toInt()
        if (idx in 0 until days) vals[idx] = d.urge.toFloat()
    }
    Bars(vals, 5f, cs.error, cs.surfaceContainerHighest)
}

@Composable
private fun Bars(values: FloatArray, max: Float, color: Color, track: Color) {
    Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
        values.forEach { v ->
            val frac = if (max == 0f) 0f else (v / max).coerceIn(0f, 1f)
            Box(Modifier.weight(1f).height((100 * (if (v == 0f) 0.04f else frac)).dp).clip(RoundedCornerShape(5.dp))
                .background(if (v == 0f) track else color))
        }
    }
}

private fun todayStart(): Long {
    val c = Calendar.getInstance()
    c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}
