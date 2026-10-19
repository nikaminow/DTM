package cloud.amnezia.dtm.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class CardGroup(val label: String) {
    CRISIS("Сейчас"), TESTS("Тесты"), DBT("ДПТ"), CBT("КПТ"), GROUND("Заземление"),
    ESCAPE("Разрыв"), PLAN("Планы"), SAFETY("Безопасность"), TEEN("До 18"), HELP("Телефоны")
}

data class HelpCard(
    val group: CardGroup,
    val title: String,
    val hint: String,
    val steps: List<String>,
    val icon: ImageVector,
    val phones: List<Pair<String, String>> = emptyList(),
)

val HELP_CARDS = listOf(
    // ---- Сейчас: острые состояния ----
    HelpCard(CardGroup.CRISIS, "Паническая атака", "Сердце колотится, не хватает воздуха",
        listOf(
            "Это паника. Она неприятна, но не опасна и пройдёт — обычно за 10–20 минут.",
            "Выдох длиннее вдоха: вдох на 4 счёта, выдох на 6–8. Повторите 6 раз.",
            "Поставьте стопы на пол, почувствуйте опору.",
            "Назовите вслух 3 предмета вокруг и их цвет.",
            "Скажите себе: «Я в безопасности прямо сейчас. Это волна, она спадёт»."
        ), Icons.Filled.Air),
    HelpCard(CardGroup.CRISIS, "Флешбэк", "Прошлое ощущается как настоящее",
        listOf(
            "Скажите себе: «Это воспоминание. Оно уже произошло. Сейчас — другое время».",
            "Назовите сегодняшнюю дату, место, где вы находитесь, и свой возраст.",
            "Возьмите в руку что-то холодное или с сильной текстурой.",
            "Оглядите комнату и найдите 5 вещей, которых не было тогда.",
            "Если рядом безопасный человек — попросите поговорить с вами о чём-то нейтральном."
        ), Icons.Filled.Waves),
    HelpCard(CardGroup.CRISIS, "Диссоциация", "Ощущение нереальности, «как во сне»",
        listOf(
            "Сожмите и разожмите кулаки 10 раз, замечая напряжение.",
            "Умойтесь холодной водой или подержите лёд.",
            "Понюхайте что-то резкое: мяту, кофе, цитрус.",
            "Пошевелите пальцами ног и назовите, что чувствуете.",
            "Двигайтесь: пройдитесь, потопайте, потянитесь."
        ), Icons.Filled.Spa),
    HelpCard(CardGroup.CRISIS, "Сразу после инцидента", "Что сделать, когда стало чуть безопаснее",
        listOf(
            "Если есть угроза жизни — 112.",
            "Обратитесь в травмпункт: побои фиксируются в медицинской карте — это важное доказательство.",
            "Сфотографируйте повреждения с датой, добавьте фото в Сейф.",
            "Запишите в Дневник: что произошло, когда, где, кто видел. Свежая память точнее.",
            "Сохраните угрожающие сообщения — скриншоты тоже в Сейф.",
            "Свяжитесь с доверенным человеком или кризисным центром."
        ), Icons.Filled.Shield),

    // ---- ДПТ ----
    HelpCard(CardGroup.DBT, "TIPP — быстро снизить накал", "Когда эмоция на пределе",
        listOf(
            "T — Температура: опустите лицо в холодную воду на 30 секунд или приложите лёд к глазам и скулам.",
            "I — Интенсивная нагрузка: 1–2 минуты приседаний, бега на месте, прыжков.",
            "P — Темповое дыхание: выдох длиннее вдоха, около 5–6 вдохов в минуту.",
            "P — Парное расслабление мышц: напрягите группу мышц на вдохе, расслабьте на выдохе."
        ), Icons.Filled.Psychology),
    HelpCard(CardGroup.DBT, "STOP", "Не действовать на импульсе",
        listOf(
            "S — Стоп. Замрите, ничего не делайте.",
            "T — Шаг назад. Отойдите, сделайте вдох.",
            "O — Наблюдайте: что происходит вокруг и внутри, без оценки.",
            "P — Действуйте осознанно: что поможет мне сейчас и в долгосрочной перспективе?"
        ), Icons.Filled.Psychology),
    HelpCard(CardGroup.DBT, "ACCEPTS — переждать бурю", "Отвлечься, чтобы не навредить себе",
        listOf(
            "A — Активности: займитесь чем-то, что требует внимания.",
            "C — Вклад: помогите кому-то, сделайте что-то доброе.",
            "C — Сравнение: вспомните трудности, которые уже остались позади.",
            "E — Эмоции наоборот: включите смешное видео, спокойную музыку.",
            "P — Отодвинуть: мысленно положите проблему в коробку до завтра.",
            "T — Мысли: считайте до 100, вспоминайте слова песни.",
            "S — Ощущения: тёплый душ, плед, кубик льда в руке."
        ), Icons.Filled.Psychology),
    HelpCard(CardGroup.DBT, "Самоуспокоение 5 чувств", "Мягко позаботиться о себе",
        listOf(
            "Зрение: посмотрите на что-то красивое — небо, растение, фото.",
            "Слух: спокойная музыка, звуки природы.",
            "Обоняние: любимый аромат, чай, крем.",
            "Вкус: медленно съешьте что-то приятное, замечая вкус.",
            "Осязание: мягкая ткань, тёплая кружка, объятие с подушкой."
        ), Icons.Filled.FavoriteBorder),
    HelpCard(CardGroup.DBT, "Радикальное принятие", "Когда ситуацию нельзя изменить прямо сейчас",
        listOf(
            "Принять — не значит одобрить. Это значит признать: «Так есть сейчас».",
            "Заметьте сопротивление: «Это несправедливо», «Так не должно быть».",
            "Скажите: «Я не могу изменить то, что уже случилось. Я могу выбрать следующий шаг».",
            "Расслабьте плечи и руки — телу тоже нужно принятие.",
            "Подумайте, какой один маленький шаг доступен сегодня."
        ), Icons.Filled.Psychology),
    HelpCard(CardGroup.DBT, "DEAR MAN — сказать о потребности", "Просить или отказывать уверенно",
        listOf(
            "D — Опишите факты без оценок.",
            "E — Выразите чувства: «Я чувствую…».",
            "A — Заявите, чего хотите, прямо.",
            "R — Подкрепите: объясните, чем это хорошо.",
            "M — Сохраняйте фокус, повторяйте просьбу спокойно.",
            "A — Держитесь уверенно: голос, осанка.",
            "N — Договаривайтесь: что вы готовы уступить?",
            "Важно: в опасной ситуации безопасность важнее разговора."
        ), Icons.Filled.Psychology),

    // ---- КПТ ----
    HelpCard(CardGroup.CBT, "Дневник мыслей", "Разобрать тяжёлую мысль по шагам",
        listOf(
            "Ситуация: что произошло? Только факты.",
            "Автоматическая мысль: что пронеслось в голове?",
            "Эмоция и её сила от 0 до 100.",
            "Доказательства «за» эту мысль.",
            "Доказательства «против».",
            "Сбалансированная мысль: как можно посмотреть иначе?",
            "Оцените эмоцию снова. Даже −10 — это уже движение."
        ), Icons.Filled.Psychology),
    HelpCard(CardGroup.CBT, "Когнитивные искажения", "Ловушки мышления, которые усиливает насилие",
        listOf(
            "Персонализация: «Это моя вина, что со мной так обращаются». Ответственность за насилие — на том, кто его совершает.",
            "Чтение мыслей: «Все считают меня слабым человеком».",
            "Катастрофизация: «Я не справлюсь, всё рухнет».",
            "Чёрно-белое мышление: «Либо идеально, либо никак».",
            "Долженствования: «Мне нужно было терпеть».",
            "Обесценивание: «Мне не так уж плохо, другим хуже».",
            "Подумайте, какие слова поддержки нашлись бы для близкого человека в такой ситуации. Скажите их себе."
        ), Icons.Filled.Psychology),
    HelpCard(CardGroup.CBT, "Проверка реальности", "Против газлайтинга",
        listOf(
            "Запишите, что вы помните, сразу и с датой — в Дневник.",
            "Сверьтесь с записями, фото, сообщениями в Сейфе.",
            "Вопрос: «Есть ли у меня факты, а не только чужие слова?»",
            "Вы имеете право доверять своим ощущениям и памяти.",
            "Обсудите ситуацию с человеком вне этих отношений."
        ), Icons.Filled.Shield),
    HelpCard(CardGroup.CBT, "Декатастрофизация", "Когда тревога рисует худшее",
        listOf(
            "Чего именно я боюсь?",
            "Какова реальная вероятность от 0 до 100%?",
            "Если это случится — что я смогу сделать? Кто поможет?",
            "Какой самый вероятный исход?",
            "Что я могу сделать уже сегодня?"
        ), Icons.Filled.Psychology),

    // ---- Заземление ----
    HelpCard(CardGroup.GROUND, "5-4-3-2-1", "Вернуться в «здесь и сейчас»",
        listOf(
            "Назовите 5 вещей, которые видите.",
            "4 — которые можете потрогать.",
            "3 — которые слышите.",
            "2 — которые чувствуете по запаху.",
            "1 — вкус во рту.",
            "Медленно выдохните."
        ), Icons.Filled.Spa),
    HelpCard(CardGroup.GROUND, "Квадратное дыхание", "4 стороны по 4 счёта",
        listOf(
            "Вдох — 4 счёта.",
            "Задержка — 4.",
            "Выдох — 4.",
            "Задержка — 4.",
            "Повторите 4–6 кругов. Если задержка тяжела — уберите её."
        ), Icons.Filled.Air),
    HelpCard(CardGroup.GROUND, "Бабочка", "Двусторонняя стимуляция для успокоения",
        listOf(
            "Скрестите руки на груди, ладони — на плечах.",
            "Поочерёдно похлопывайте левой и правой рукой.",
            "Дышите медленно, 1–2 минуты.",
            "Можно представить безопасное место."
        ), Icons.Filled.FavoriteBorder),
    HelpCard(CardGroup.GROUND, "Безопасное место", "Образ, к которому можно возвращаться",
        listOf(
            "Представьте место, где вам спокойно — реальное или воображаемое.",
            "Что вы там видите? Какие цвета?",
            "Какие звуки? Какая температура воздуха?",
            "Что чувствует тело в этом месте?",
            "Придумайте слово-якорь, чтобы быстро вспоминать это место."
        ), Icons.Filled.Spa),

    // ---- Безопасность ----
    HelpCard(CardGroup.SAFETY, "План безопасности", "Подготовиться заранее",
        listOf(
            "Определите безопасные комнаты дома — с выходом, без острых предметов.",
            "Договоритесь с доверенным человеком о кодовом слове «мне нужна помощь».",
            "Запомните наизусть 1–2 важных номера.",
            "Держите телефон заряженным, деньги — в доступном месте.",
            "Решите заранее, куда можно уйти: друзья, родственники, кризисный центр."
        ), Icons.Filled.Shield),
    HelpCard(CardGroup.SAFETY, "Тревожный чемоданчик", "Что собрать и где спрятать",
        listOf(
            "Документы или их копии: паспорт, СНИЛС, полис, свидетельства, документы детей.",
            "Деньги, запасная банковская карта.",
            "Ключи, зарядка, лекарства на несколько дней.",
            "Смена одежды.",
            "Храните у доверенного человека или в надёжном месте вне дома.",
            "Сканы документов — в Сейфе приложения."
        ), Icons.Filled.Shield),
    HelpCard(CardGroup.SAFETY, "Цифровая безопасность", "Чтобы вас не отслеживали",
        listOf(
            "Смените пароли от почты и соцсетей, включите двухфакторную защиту.",
            "Проверьте, нет ли на телефоне незнакомых приложений и доступа к геолокации у других.",
            "Выйдите из аккаунтов на чужих устройствах.",
            "Проверьте настройки «поделиться геопозицией».",
            "Используйте PIN, который никто не знает."
        ), Icons.Filled.Shield),

    // ---- Права до 18 лет (РФ). Не юридическая консультация. ----
    HelpCard(CardGroup.TEEN, "Твои права в семье", "Что говорит Семейный кодекс",
        listOf(
            "Родители обязаны воспитывать без пренебрежительного, жестокого, грубого и унижающего обращения, оскорблений и эксплуатации (ст. 65 СК РФ).",
            "У тебя есть право на защиту от злоупотреблений со стороны родителей (ст. 56 СК РФ).",
            "Можно самостоятельно обратиться в орган опеки и попечительства в любом возрасте, а с 14 лет — в суд (ст. 56 СК РФ).",
            "С 10 лет твоё мнение обязаны учитывать при решениях, которые тебя касаются (ст. 57 СК РФ).",
            "Это общая информация, а не юридическая консультация. Бесплатно помогут юридические клиники вузов и детский телефон доверия."
        ), Icons.Filled.Gavel),
    HelpCard(CardGroup.TEEN, "Деньги и вещи", "Экономическое давление",
        listOf(
            "С 14 лет свой заработок, стипендию и другие доходы можно тратить самостоятельно (ст. 26 ГК РФ).",
            "Родители обязаны содержать детей до 18 лет (ст. 80 СК РФ): еда, одежда, жильё, лечение, учёба.",
            "Если деньги или вещи постоянно отбирают в наказание или чтобы контролировать — это может быть формой насилия. Запиши такие случаи в Дневник."
        ), Icons.Filled.Gavel),
    HelpCard(CardGroup.TEEN, "Переписка и телефон", "Где забота, а где контроль",
        listOf(
            "Тайна переписки и телефонных разговоров защищена Конституцией для всех, включая подростков (ст. 23).",
            "Родители отвечают за твою безопасность, поэтому разговор о правилах пользования телефоном — нормален.",
            "Тайное чтение переписки, высмеивание её содержимого или постоянная слежка — уже контроль, а не забота.",
            "Включи блокировку экрана и не сообщай пароли. В приложении есть кнопка быстрого выхода."
        ), Icons.Filled.Gavel),
    HelpCard(CardGroup.TEEN, "Медицинская и психологическая помощь", "Когда можно обращаться самостоятельно",
        listOf(
            "С 15 лет можно самостоятельно давать согласие на медицинскую помощь (ст. 54 ФЗ № 323 «Об основах охраны здоровья»).",
            "Это включает приём у врача-психиатра и психотерапевта.",
            "Анонимно поговорить с психологом можно по детскому телефону доверия 8-800-2000-122 в любом возрасте."
        ), Icons.Filled.Gavel),
    HelpCard(CardGroup.TEEN, "Если выгоняют из дома или опасно оставаться", "Куда идти",
        listOf(
            "При угрозе жизни — 112 или 102.",
            "Социально-реабилитационные центры для несовершеннолетних принимают в том числе по личному обращению самого подростка (ст. 13 ФЗ № 120).",
            "Можно обратиться в орган опеки и попечительства своего района.",
            "Детский телефон доверия 8-800-2000-122 подскажет ближайший центр помощи.",
            "Можно пойти к взрослому, которому доверяешь: родственнику, учителю, школьному психологу."
        ), Icons.Filled.Gavel),

    // ---- До 18: пошаговые алгоритмы ----
    HelpCard(CardGroup.TEEN, "Забрали паспорт или вещи", "Что делать по шагам",
        listOf(
            "Паспорт — твой личный документ. Удерживать чужой паспорт против воли его владельца закон не разрешает, даже родителям.",
            "Сначала спокойно попроси вернуть, лучше при свидетеле: «Верни, пожалуйста, мой паспорт, он мне нужен». Запиши в Дневник дату и ответ.",
            "Если не возвращают — можно подать заявление в полицию об удержании документа. Его обязаны принять и выдать талон-уведомление.",
            "Если паспорт не вернуть, обратись в МВД (вопросы миграции) или МФЦ: заяви об утрате и подай на новый. На время замены выдают временное удостоверение личности.",
            "Вещи, купленные на твой заработок или стипендию, с 14 лет — твои (ст. 26 ГК РФ). Сфотографируй чеки, если они есть, и сохрани в Сейф.",
            "Не знаешь, с чего начать, — позвони на детский телефон доверия 8-800-2000-122: там помогут составить план.",
        ), Icons.Filled.Gavel),
    HelpCard(CardGroup.TEEN, "Полиция не едет или встаёт на сторону родителей", "Что говорить и что требовать",
        listOf(
            "Звони 112 или 102. Говори коротко: адрес, что происходит прямо сейчас, есть ли побои или угрозы, сколько тебе лет.",
            "Фраза, которая работает: «Мне N лет, я несовершеннолетний человек, мне угрожает опасность дома, прошу направить наряд».",
            "Спроси фамилию дежурного и номер, под которым зарегистрировано сообщение. Сообщение обязаны зарегистрировать. Запиши всё в Дневник.",
            "Если наряд приехал и не помогает — попроси принять твоё заявление и пригласить инспектора по делам несовершеннолетних (ПДН).",
            "Есть побои — иди в травмпункт: повреждения зафиксируют в медицинских документах.",
            "Если в полиции отказали или ничего не сделали — можно пожаловаться в прокуратуру, в том числе онлайн через сайт прокуратуры. Приложи даты, фамилии и номер сообщения.",
        ), Icons.Filled.Gavel),
    HelpCard(CardGroup.TEEN, "Органы опеки бездействуют", "Как добиться ответа",
        listOf(
            "Обращайся письменно, а не только устно. Сделай копию и попроси поставить на ней отметку о приёме с датой.",
            "На письменное обращение обязаны ответить, обычно в течение 30 дней. Отметь дату в Дневнике.",
            "Если ответа нет или он формальный — пиши руководителю органа опеки, в комиссию по делам несовершеннолетних и защите их прав (КДН) при администрации района и в прокуратуру.",
            "Есть уполномоченный по правам ребёнка в твоём регионе — ему тоже можно написать, это бесплатно.",
            "Опиши факты: что, когда, кто видел. Приложи записи из Дневника и отчёт из приложения.",
            "Помочь составить обращение могут на детском телефоне доверия 8-800-2000-122 и в юридических клиниках вузов.",
        ), Icons.Filled.Gavel),

    // ---- Разрыв и синдром отмены ----
    HelpCard(CardGroup.ESCAPE, "Психологический манифест", "Читать при вспышках вины",
        listOf(
            "Мой уход — это не месть и не предательство. Это вынужденная самооборона.",
            "Я не обязан быть хорошим для тех, кто меня разрушал.",
            "Я не отвечаю за чувства, зависимости, бедность или одиночество взрослого человека, который причинял мне вред.",
            "Безопасность моей жизни важнее чужих манипуляций и одобрения.",
            "Тянет вернуться — это синдром отмены, а не любовь. Он пройдёт."
        ), Icons.Filled.Shield),
    HelpCard(CardGroup.ESCAPE, "Метод «серого камня»", "Если контакта не избежать",
        listOf(
            "Станьте максимально скучным и предсказуемым собеседником. Агрессор питается эмоциями — без них он теряет интерес.",
            "Отвечайте тихо, монотонно, одним-двумя словами.",
            "Готовые фразы: «Да». «Нет». «Хорошо». «Я услышал(а)».",
            "Не спорьте, не оправдывайтесь, не доказывайте правоту, не плачьте на виду.",
            "Ваши аргументы и эмоции могут использовать против вас. Берегите их."
        ), Icons.Filled.Block),
    HelpCard(CardGroup.ESCAPE, "Откат: тянет вернуться", "Когда страшно и одиноко",
        listOf(
            "Заземление: умойтесь ледяной водой, подышите — вдох на 4, выдох на 7. Это гасит панику.",
            "Факты: перечитайте в Дневнике список причин, по которым вы ушли. Память стирает плохое — список вернёт к реальности.",
            "Импульс: если рука тянется разблокировать агрессора — займите тело на 15 минут (20 приседаний, мытьё посуды, быстрая прогулка).",
            "Через 15 минут пик аффекта спадёт. Решения принимайте после этого."
        ), Icons.Filled.Waves),

    // ---- Телефоны ----
    HelpCard(CardGroup.HELP, "Экстренные службы", "Угроза жизни и здоровью",
        listOf("Звонок бесплатный, в том числе без SIM-карты и денег на счету."),
        Icons.Filled.Call,
        phones = listOf("Единый номер экстренных служб" to "112", "Полиция" to "102", "Скорая помощь" to "103")),
    HelpCard(CardGroup.HELP, "Помощь при домашнем насилии", "Консультация, поддержка, убежища",
        listOf("Номера могут меняться — уточняйте актуальные контакты местных кризисных центров."),
        Icons.Filled.Call,
        phones = listOf("Всероссийский телефон доверия, центр «Анна»" to "88007000600")),
    HelpCard(CardGroup.HELP, "Психологическая поддержка", "Если очень тяжело",
        listOf("Можно звонить, даже если не знаете, что сказать."),
        Icons.Filled.Call,
        phones = listOf("Телефон доверия для детей, подростков и родителей" to "88002000122",
            "Экстренная психологическая помощь МЧС" to "84959895050")),
)

@Composable
fun SupportScreen() {
    var group by rememberSaveable { mutableStateOf(CardGroup.CRISIS) }
    val cards = remember(group) { HELP_CARDS.filter { it.group == group } }

    ScreenScaffold(
        title = "Опора",
        subtitle = "Техники для трудных минут. Выберите то, что подходит сейчас.",
    ) {
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CardGroup.entries.forEach { g ->
                    SoftChip(g.label, g == group) { group = g }
                }
            }
        }
        if (group == CardGroup.CRISIS) {
            item { RealityCard() }
        }
        if (group == CardGroup.PLAN) {
            item {
                Text("Отмечайте пункты — прогресс сохраняется.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp))
            }
            items(CHECKLISTS, key = { it.id }) { ChecklistCard(it) }
        }
        if (group == CardGroup.TESTS) {
            item {
                Text(
                    "Тесты помогают взглянуть со стороны. Это не диагноз, ответы нигде не сохраняются.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            items(QUIZZES, key = { it.title }) { QuizCard(it) }
        } else {
            items(cards, key = { it.title }) { HelpCardView(it) }
        }
    }
}

@Composable
private fun HelpCardView(card: HelpCard) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    var open by rememberSaveable(card.title) { mutableStateOf(card.group == CardGroup.HELP) }
    val crisis = card.group == CardGroup.CRISIS
    Card(
        onClick = { open = !open },
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (open) cs.surfaceContainerHigh else cs.surfaceContainerLow,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    card.icon,
                    if (crisis) cs.tertiaryContainer else cs.secondaryContainer,
                    if (crisis) cs.onTertiaryContainer else cs.onSecondaryContainer,
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(card.title, style = MaterialTheme.typography.titleMedium)
                    Text(card.hint, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
                Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = cs.onSurfaceVariant)
            }
            if (open) {
                Spacer(Modifier.height(14.dp))
                card.steps.forEachIndexed { i, s ->
                    Row(Modifier.padding(vertical = 5.dp)) {
                        if (card.steps.size > 1) {
                            Text("${i + 1}", color = cs.primary, style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                        }
                        Text(s, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                card.phones.forEach { (label, number) ->
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        color = cs.secondaryContainer,
                        contentColor = cs.onSecondaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(label, style = MaterialTheme.typography.bodySmall)
                                Text(prettyPhone(number), style = MaterialTheme.typography.titleLarge)
                            }
                            Icon(Icons.Filled.Call, "Позвонить")
                        }
                    }
                }
            }
        }
    }
}

private fun prettyPhone(n: String): String =
    if (n.length == 11 && n.startsWith("8"))
        "8 ${n.substring(1, 4)} ${n.substring(4, 7)}-${n.substring(7, 9)}-${n.substring(9)}"
    else n
