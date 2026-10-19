package cloud.amnezia.dtm.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class Quiz(
    val title: String,
    val intro: String,
    val questions: List<String>,
    /** Пороговые уровни: при счёте ≥ порога показывается этот вывод. От большего к меньшему. */
    val results: List<QuizResult>,
)

data class QuizResult(val minScore: Int, val headline: String, val text: String)

/** Каждый вопрос — утверждение; «Да» = 1 балл, «Иногда» = 1, «Нет» = 0. */
val QUIZZES = listOf(
    Quiz(
        "Токсичные отношения или норма?",
        "Отметьте, что бывает в ваших отношениях — с партнёром, другом или в семье.",
        listOf(
            "Мне приходится следить за словами, чтобы не вызвать вспышку злости.",
            "Мне говорят, что я всё придумываю и неправильно всё помню.",
            "Меня высмеивают или унижают, в том числе при других.",
            "Мне запрещают общаться с друзьями или родными.",
            "Проверяют мой телефон, переписку, требуют отчёта, где я.",
            "После ссор я чувствую, что виновато всегда только я.",
            "Мне угрожали — словами, жестами или силой.",
            "Я отказываюсь от своих планов и желаний, лишь бы не было конфликта.",
        ),
        listOf(
            QuizResult(5, "Похоже на нездоровые отношения",
                "Многое из отмеченного — признаки контроля и насилия, а не любви. Вы не виноваты в чужой агрессии. " +
                    "Посмотрите карточки в разделах «Безопасность» и «Телефоны», подумайте о разговоре с доверенным человеком или специалистом."),
            QuizResult(2, "Есть тревожные сигналы",
                "Отдельные пункты стоит воспринять серьёзно. Понаблюдайте, как часто это повторяется, и фиксируйте случаи в Дневнике. " +
                    "Нормальные отношения не строятся на страхе и контроле."),
            QuizResult(0, "Явных признаков контроля немного",
                "Это хороший знак. Если что-то всё же тревожит — доверяйте своим ощущениям, они важны."),
        ),
    ),
    Quiz(
        "Забота или контроль?",
        "Про отношения с родителями или опекунами.",
        listOf(
            "Запрещают выходить из дома без реальной причины, кроме «я так сказал».",
            "Читают переписку и высмеивают её.",
            "Отбирают деньги или вещи в наказание.",
            "Оскорбляют, кричат, называют обидными словами.",
            "Угрожают выгнать из дома или причинить вред.",
            "Запрещают общаться с друзьями.",
            "Применяют физическую силу.",
        ),
        listOf(
            QuizResult(4, "Это выходит за рамки заботы",
                "Забота защищает, а не унижает и не пугает. Отмеченное может быть насилием. " +
                    "В разделе «До 18» есть ваши права, в «Телефонах» — куда можно обратиться анонимно."),
            QuizResult(2, "Стоит присмотреться",
                "Граница между заботой и контролем — в уважении к вам. Фиксируйте в Дневнике то, что ранит, и при случае обсудите со взрослым, которому доверяете."),
            QuizResult(0, "Больше похоже на заботу",
                "Правила и беспокойство родителей — это нормально, когда они не держатся на страхе. Если что-то тревожит, вы всегда можете позвонить на телефон доверия."),
        ),
    ),
    Quiz(
        "Что делать, если родители залезли в телефон?",
        "Короткая самопроверка: готовы ли вы к такой ситуации.",
        listOf(
            "На телефоне стоит блокировка экрана, и пароль знаю только я.",
            "Я знаю про кнопку быстрого выхода в этом приложении.",
            "Важные скриншоты и документы я храню в Сейфе, а не в галерее.",
            "У меня есть взрослый или друг, к которому можно обратиться.",
            "Я знаю телефон доверия наизусть или записал его в надёжном месте.",
        ),
        listOf(
            QuizResult(4, "Вы хорошо подготовлены",
                "Отлично. На всякий случай ещё раз загляните в «Цифровую безопасность» в разделе «Безопасность»."),
            QuizResult(2, "Частично готовы",
                "Стоит закрыть пробелы: поставить блокировку, перенести важное в Сейф, запомнить телефон доверия. Карточки в «Безопасности» помогут."),
            QuizResult(0, "Есть над чем поработать",
                "Начните с простого: блокировка экрана и Сейф для важных файлов. Раздел «Безопасность» подскажет шаги."),
        ),
    ),
)

@Composable
fun QuizCard(quiz: Quiz) {
    val cs = MaterialTheme.colorScheme
    var open by remember(quiz.title) { mutableStateOf(false) }
    // вопрос -> индекс варианта (0=Да, 1=Иногда, 2=Нет)
    val answers = remember(quiz.title) { mutableStateMapOf<Int, Int>() }
    val options = listOf("Да", "Иногда", "Нет")
    val scores = listOf(1, 1, 0)
    var submitted by remember(quiz.title) { mutableStateOf(false) }

    Card(
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = if (open) cs.surfaceContainerHigh else cs.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
        onClick = { if (!open) open = true },
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Filled.Quiz)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(quiz.title, style = MaterialTheme.typography.titleMedium)
                    if (!open) Text("${quiz.questions.size} вопросов · 1 минута",
                        style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
            }

            if (open) {
                Spacer(Modifier.height(10.dp))
                Text(quiz.intro, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                quiz.questions.forEachIndexed { qi, q ->
                    Spacer(Modifier.height(10.dp))
                    Text(q, style = MaterialTheme.typography.bodyLarge)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        options.forEachIndexed { oi, label ->
                            val selected = answers[qi] == oi
                            Row(
                                Modifier.weight(1f).selectable(selected = selected) { answers[qi] = oi },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = selected, onClick = { answers[qi] = oi })
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                if (!submitted) {
                    Button(
                        onClick = { submitted = true },
                        enabled = answers.size >= quiz.questions.size,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) { Text(if (answers.size >= quiz.questions.size) "Показать результат" else "Ответьте на все вопросы (${answers.size}/${quiz.questions.size})") }
                } else {
                    val score = quiz.questions.indices.sumOf { scores.getOrElse(answers[it] ?: 2) { 0 } }
                    val result = quiz.results.first { score >= it.minScore }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cs.secondaryContainer, contentColor = cs.onSecondaryContainer),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(result.headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(result.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { answers.clear(); submitted = false }) {
                        Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Пройти заново")
                    }
                }
            }
        }
    }
}
