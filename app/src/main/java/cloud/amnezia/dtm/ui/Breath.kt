package cloud.amnezia.dtm.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cloud.amnezia.dtm.data.Tracker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class BreathMode(val title: String, val phases: List<Pair<String, Int>>)

private val MODES = listOf(
    BreathMode("Квадрат 4-4-4-4", listOf("Вдох" to 4, "Пауза" to 4, "Выдох" to 4, "Пауза" to 4)),
    BreathMode("Успокоение 4-7-8", listOf("Вдох" to 4, "Пауза" to 7, "Выдох" to 8)),
    BreathMode("Мягкое 4-6", listOf("Вдох" to 4, "Выдох" to 6)),
)

@Composable
fun BreathScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    var mode by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf("Готово к началу") }
    var left by remember { mutableIntStateOf(0) }
    var cycles by remember { mutableIntStateOf(0) }
    val scale = remember { Animatable(0.55f) }

    LaunchedEffect(running, mode) {
        if (!running) { scale.animateTo(0.55f, tween(600)); return@LaunchedEffect }
        while (running) {
            for ((name, sec) in MODES[mode].phases) {
                phase = name
                val target = when (name) { "Вдох" -> 1f; "Выдох" -> 0.55f; else -> scale.value }
                launch { scale.animateTo(target, tween(sec * 1000, easing = LinearEasing)) }
                for (s in sec downTo 1) { left = s; delay(1000) }
            }
            cycles++
        }
    }

    ScreenScaffold(title = "Дыхание", subtitle = "Следите за кругом: он растёт на вдохе и сжимается на выдохе.", onBack = onBack, bottomSpace = false) {
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MODES.forEachIndexed { i, m -> SoftChip(m.title, mode == i) { mode = i; cycles = 0 } }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(280.dp).scale(scale.value).clip(CircleShape).background(cs.primaryContainer))
                Box(Modifier.size(180.dp).scale(scale.value).clip(CircleShape).background(cs.primary.copy(alpha = 0.25f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(phase, style = MaterialTheme.typography.headlineMedium, color = cs.onPrimaryContainer)
                    if (running) Text("$left", style = MaterialTheme.typography.displaySmall, color = cs.onPrimaryContainer)
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Циклов: $cycles", style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
                PillButton(
                    if (running) "Завершить" else "Начать",
                    {
                        if (running && cycles > 0) scope.launch { Tracker.addLog(context, "Дыхание", "${MODES[mode].title}, циклов: $cycles") }
                        running = !running
                        if (!running) phase = "Готово к началу"
                    },
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        }
    }
}

val WARM_WORDS = listOf(
    "То, что с вами происходит, — не ваша вина.",
    "Ваши чувства настоящие и важные.",
    "Можно отдыхать, даже если не всё сделано.",
    "Маленький шаг — тоже шаг.",
    "Вы заслуживаете безопасности и бережного отношения.",
    "Сегодня достаточно просто прожить этот день.",
    "Просить о помощи — это сила.",
    "Ваша память и ощущения имеют значение.",
    "Вы справлялись с трудным раньше — и это видно.",
    "Можно говорить «нет».",
)
