package cloud.amnezia.dtm.ui

import cloud.amnezia.dtm.tr

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale

private data class Guide(val title: String, val hint: String, val steps: List<String>)

private val GUIDES = listOf(
    Guide("5-4-3-2-1", tr("Вернуться в здесь и сейчас"), listOf(
        tr("Устройтесь поудобнее. Мы сделаем это медленно, вместе."),
        tr("Найдите глазами пять предметов вокруг. Назовите каждый про себя."),
        tr("Прислушайтесь. Найдите четыре звука — близких или далёких."),
        tr("Коснитесь трёх предметов. Почувствуйте, тёплые они или холодные, гладкие или шершавые."),
        tr("Найдите два запаха. Любых."),
        tr("Обратите внимание на один вкус во рту."),
        tr("Сделайте медленный вдох. Вы здесь. Вы в безопасности в этот момент."),
    )),
    Guide(tr("Дыхание 4-7-8"), tr("Успокоить тело"), listOf(
        tr("Сядьте удобно, опустите плечи."),
        tr("Медленно вдохните носом на четыре счёта. Раз. Два. Три. Четыре."),
        tr("Задержите дыхание. Раз. Два. Три. Четыре. Пять. Шесть. Семь."),
        tr("Медленно выдохните ртом на восемь счётов."),
        tr("Ещё раз. Вдох на четыре."),
        tr("Задержка на семь."),
        tr("Выдох на восемь. С каждым выдохом тело расслабляется."),
    )),
    Guide(tr("При флешбэке"), tr("Прошлое осталось в прошлом"), listOf(
        tr("То, что вы чувствуете, — это воспоминание. Оно уже закончилось."),
        tr("Назовите сегодняшнюю дату. Назовите место, где вы сейчас."),
        tr("Оглядитесь и найдите пять вещей, которых не было тогда."),
        tr("Почувствуйте опору под ногами. Пошевелите пальцами ног."),
        tr("Вы выросли. Сейчас вы в безопасности. Это чувство пройдёт."),
    )),
)

@Composable
fun AudioGuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    var selected by remember { mutableStateOf(0) }
    var speaking by remember { mutableStateOf(false) }
    var line by remember { mutableIntStateOf(-1) }
    var ready by remember { mutableStateOf(false) }

    val tts = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = java.util.Locale.getDefault()
                engine?.setSpeechRate(0.9f)
                ready = true
            }
        }
        engine
    }
    DisposableEffect(Unit) {
        tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(id: String?) { id?.toIntOrNull()?.let { line = it } }
            override fun onDone(id: String?) {
                if (id == "last") { speaking = false; line = -1 }
            }
            @Deprecated("") override fun onError(id: String?) {}
        })
        onDispose { tts.stop(); tts.shutdown() }
    }

    fun play() {
        val g = GUIDES[selected]
        tts.stop()
        speaking = true
        g.steps.forEachIndexed { i, s ->
            val id = if (i == g.steps.lastIndex) "last" else "$i"
            tts.speak(s, if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, id)
        }
    }
    fun stop() { tts.stop(); speaking = false; line = -1 }

    ScreenScaffold(
        title = tr("Голосовой гид"),
        subtitle = tr("Спокойный голос проведёт вас через упражнение. Руки свободны, можно закрыть глаза."),
        onBack = onBack, bottomSpace = false,
    ) {
        item {
            androidx.compose.foundation.layout.Row(
                Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GUIDES.forEachIndexed { i, g -> SoftChip(g.title, selected == i) { if (!speaking) selected = i } }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(150.dp).clip(CircleShape).background(if (speaking) cs.tertiary else cs.primary)
                        .softClickable(ready) { if (speaking) stop() else play() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (speaking) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (speaking) tr("Стоп") else tr("Начать"),
                        tint = if (speaking) cs.onTertiary else cs.onPrimary, modifier = Modifier.size(64.dp))
                }
            }
        }
        item {
            Text(
                if (line >= 0) GUIDES[selected].steps[line] else if (!ready) tr("Готовлю голос…") else tr("Нажмите, чтобы начать"),
                Modifier.fillMaxWidth().padding(horizontal = 28.dp),
                style = MaterialTheme.typography.titleMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = cs.onSurface,
            )
        }
        item {
            if (!ready) Text(tr("Если голос не появился, установите «Синтез речи» в настройках телефона (обычно Google TTS)."),
                Modifier.fillMaxWidth().padding(24.dp), style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
