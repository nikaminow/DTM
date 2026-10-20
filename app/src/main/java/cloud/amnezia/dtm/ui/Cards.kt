package cloud.amnezia.dtm.ui

import cloud.amnezia.dtm.tr

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
    CRISIS(tr("Сейчас")), TESTS(tr("Тесты")), DBT(tr("ДПТ")), CBT(tr("КПТ")), GROUND(tr("Заземление")),
    ESCAPE(tr("Разрыв")), PLAN(tr("Планы")), SAFETY(tr("Безопасность")), TEEN(tr("До 18")), HELP(tr("Телефоны"))
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
    HelpCard(CardGroup.CRISIS, tr("Паническая атака"), tr("Сердце колотится, не хватает воздуха"),
        listOf(
            tr("Это паника. Она неприятна, но не опасна и пройдёт — обычно за 10–20 минут."),
            tr("Выдох длиннее вдоха: вдох на 4 счёта, выдох на 6–8. Повторите 6 раз."),
            tr("Поставьте стопы на пол, почувствуйте опору."),
            tr("Назовите вслух 3 предмета вокруг и их цвет."),
            tr("Скажите себе: «Я в безопасности прямо сейчас. Это волна, она спадёт».")
        ), Icons.Filled.Air),
    HelpCard(CardGroup.CRISIS, tr("Флешбэк"), tr("Прошлое ощущается как настоящее"),
        listOf(
            tr("Скажите себе: «Это воспоминание. Оно уже произошло. Сейчас — другое время»."),
            tr("Назовите сегодняшнюю дату, место, где вы находитесь, и свой возраст."),
            tr("Возьмите в руку что-то холодное или с сильной текстурой."),
            tr("Оглядите комнату и найдите 5 вещей, которых не было тогда."),
            tr("Если рядом безопасный человек — попросите поговорить с вами о чём-то нейтральном.")
        ), Icons.Filled.Waves),
    HelpCard(CardGroup.CRISIS, tr("Диссоциация"), tr("Ощущение нереальности, «как во сне»"),
        listOf(
            tr("Сожмите и разожмите кулаки 10 раз, замечая напряжение."),
            tr("Умойтесь холодной водой или подержите лёд."),
            tr("Понюхайте что-то резкое: мяту, кофе, цитрус."),
            tr("Пошевелите пальцами ног и назовите, что чувствуете."),
            tr("Двигайтесь: пройдитесь, потопайте, потянитесь.")
        ), Icons.Filled.Spa),
    HelpCard(CardGroup.CRISIS, tr("Сразу после инцидента"), tr("Что сделать, когда стало чуть безопаснее"),
        listOf(
            tr("Если есть угроза жизни — 112."),
            tr("Обратитесь в травмпункт: побои фиксируются в медицинской карте — это важное доказательство."),
            tr("Сфотографируйте повреждения с датой, добавьте фото в Сейф."),
            tr("Запишите в Дневник: что произошло, когда, где, кто видел. Свежая память точнее."),
            tr("Сохраните угрожающие сообщения — скриншоты тоже в Сейф."),
            tr("Свяжитесь с доверенным человеком или кризисным центром.")
        ), Icons.Filled.Shield),

    // ---- ДПТ ----
    HelpCard(CardGroup.DBT, tr("TIPP — быстро снизить накал"), tr("Когда эмоция на пределе"),
        listOf(
            tr("T — Температура: опустите лицо в холодную воду на 30 секунд или приложите лёд к глазам и скулам."),
            tr("I — Интенсивная нагрузка: 1–2 минуты приседаний, бега на месте, прыжков."),
            tr("P — Темповое дыхание: выдох длиннее вдоха, около 5–6 вдохов в минуту."),
            tr("P — Парное расслабление мышц: напрягите группу мышц на вдохе, расслабьте на выдохе.")
        ), Icons.Filled.Psychology),
    HelpCard(CardGroup.DBT, "STOP", tr("Не действовать на импульсе"),
        listOf(
            tr("S — Стоп. Замрите, ничего не делайте."),
            tr("T — Шаг назад. Отойдите, сделайте вдох."),
            tr("O — Наблюдайте: что происходит вокруг и внутри, без оценки."),
            tr("P — Действуйте осознанно: что поможет мне сейчас и в долгосрочной перспективе?")
        ), Icons.Filled.Psychology),
    HelpCard(CardGroup.DBT, tr("ACCEPTS — переждать бурю"), tr("Отвлечься, чтобы не навредить себе"),
        listOf(
            tr("A — Активности: займитесь чем-то, что требует внимания."),
            tr("C — Вклад: помогите кому-то, сделайте что-то доброе."),
            tr("C — Сравнение: вспомните трудности, которые уже остались позади."),
            tr("E — Эмоции наоборот: включите смешное видео, спокойную музыку."),
            tr("P — Отодвинуть: мысленно положите проблему в коробку до завтра."),
            tr("T — Мысли: считайте до 100, вспоминайте слова песни."),
            tr("S — Ощущения: тёплый душ, плед, кубик льда в руке.")
        ), Icons.Filled.Psychology),
    HelpCard(CardGroup.DBT, tr("Самоуспокоение 5 чувств"), tr("Мягко позаботиться о себе"),
        listOf(
            tr("Зрение: посмотрите на что-то красивое — небо, растение, фото."),
            tr("Слух: спокойная музыка, звуки природы."),
            tr("Обоняние: любимый аромат, чай, крем."),
            tr("Вкус: медленно съешьте что-то приятное, замечая вкус."),
            tr("Осязание: мягкая ткань, тёплая кружка, объятие с подушкой.")
        ), Icons.Filled.FavoriteBorder),
    HelpCard(CardGroup.DBT, tr("Радикальное принятие"), tr("Когда ситуацию нельзя изменить прямо сейчас"),
        listOf(
            tr("Принять — не значит одобрить. Это значит признать: «Так есть сейчас»."),
            tr("Заметьте сопротивление: «Это несправедливо», «Так не должно быть»."),
            tr("Скажите: «Я не могу изменить то, что уже случилось. Я могу выбрать следующий шаг»."),
            tr("Расслабьте плечи и руки — телу тоже нужно принятие."),
            tr("Подумайте, какой один маленький шаг доступен сегодня.")
        ), Icons.Filled.Psychology),
    HelpCard(CardGroup.DBT, tr("DEAR MAN — сказать о потребности"), tr("Просить или отказывать уверенно"),
        listOf(
            tr("D — Опишите факты без оценок."),
            tr("E — Выразите чувства: «Я чувствую…»."),
            tr("A — Заявите, чего хотите, прямо."),
            tr("R — Подкрепите: объясните, чем это хорошо."),
            tr("M — Сохраняйте фокус, повторяйте просьбу спокойно."),
            tr("A — Держитесь уверенно: голос, осанка."),
            tr("N — Договаривайтесь: что вы готовы уступить?"),
            tr("Важно: в опасной ситуации безопасность важнее разговора.")
        ), Icons.Filled.Psychology),

    // ---- КПТ ----
    HelpCard(CardGroup.CBT, tr("Дневник мыслей"), tr("Разобрать тяжёлую мысль по шагам"),
        listOf(
            tr("Ситуация: что произошло? Только факты."),
            tr("Автоматическая мысль: что пронеслось в голове?"),
            tr("Эмоция и её сила от 0 до 100."),
            tr("Доказательства «за» эту мысль."),
            tr("Доказательства «против»."),
            tr("Сбалансированная мысль: как можно посмотреть иначе?"),
            tr("Оцените эмоцию снова. Даже −10 — это уже движение.")
        ), Icons.Filled.Psychology),
    HelpCard(CardGroup.CBT, tr("Когнитивные искажения"), tr("Ловушки мышления, которые усиливает насилие"),
        listOf(
            tr("Персонализация: «Это моя вина, что со мной так обращаются». Ответственность за насилие — на том, кто его совершает."),
            tr("Чтение мыслей: «Все считают меня слабым человеком»."),
            tr("Катастрофизация: «Я не справлюсь, всё рухнет»."),
            tr("Чёрно-белое мышление: «Либо идеально, либо никак»."),
            tr("Долженствования: «Мне нужно было терпеть»."),
            tr("Обесценивание: «Мне не так уж плохо, другим хуже»."),
            tr("Подумайте, какие слова поддержки нашлись бы для близкого человека в такой ситуации. Скажите их себе.")
        ), Icons.Filled.Psychology),
    HelpCard(CardGroup.CBT, tr("Проверка реальности"), tr("Против газлайтинга"),
        listOf(
            tr("Запишите, что вы помните, сразу и с датой — в Дневник."),
            tr("Сверьтесь с записями, фото, сообщениями в Сейфе."),
            tr("Вопрос: «Есть ли у меня факты, а не только чужие слова?»"),
            tr("Вы имеете право доверять своим ощущениям и памяти."),
            tr("Обсудите ситуацию с человеком вне этих отношений.")
        ), Icons.Filled.Shield),
    HelpCard(CardGroup.CBT, tr("Декатастрофизация"), tr("Когда тревога рисует худшее"),
        listOf(
            tr("Чего именно я боюсь?"),
            tr("Какова реальная вероятность от 0 до 100%?"),
            tr("Если это случится — что я смогу сделать? Кто поможет?"),
            tr("Какой самый вероятный исход?"),
            tr("Что я могу сделать уже сегодня?")
        ), Icons.Filled.Psychology),

    // ---- Заземление ----
    HelpCard(CardGroup.GROUND, "5-4-3-2-1", tr("Вернуться в «здесь и сейчас»"),
        listOf(
            tr("Назовите 5 вещей, которые видите."),
            tr("4 — которые можете потрогать."),
            tr("3 — которые слышите."),
            tr("2 — которые чувствуете по запаху."),
            tr("1 — вкус во рту."),
            tr("Медленно выдохните.")
        ), Icons.Filled.Spa),
    HelpCard(CardGroup.GROUND, tr("Квадратное дыхание"), tr("4 стороны по 4 счёта"),
        listOf(
            tr("Вдох — 4 счёта."),
            tr("Задержка — 4."),
            tr("Выдох — 4."),
            tr("Задержка — 4."),
            tr("Повторите 4–6 кругов. Если задержка тяжела — уберите её.")
        ), Icons.Filled.Air),
    HelpCard(CardGroup.GROUND, tr("Бабочка"), tr("Двусторонняя стимуляция для успокоения"),
        listOf(
            tr("Скрестите руки на груди, ладони — на плечах."),
            tr("Поочерёдно похлопывайте левой и правой рукой."),
            tr("Дышите медленно, 1–2 минуты."),
            tr("Можно представить безопасное место.")
        ), Icons.Filled.FavoriteBorder),
    HelpCard(CardGroup.GROUND, tr("Безопасное место"), tr("Образ, к которому можно возвращаться"),
        listOf(
            tr("Представьте место, где вам спокойно — реальное или воображаемое."),
            tr("Что вы там видите? Какие цвета?"),
            tr("Какие звуки? Какая температура воздуха?"),
            tr("Что чувствует тело в этом месте?"),
            tr("Придумайте слово-якорь, чтобы быстро вспоминать это место.")
        ), Icons.Filled.Spa),

    // ---- Безопасность ----
    HelpCard(CardGroup.SAFETY, tr("План безопасности"), tr("Подготовиться заранее"),
        listOf(
            tr("Определите безопасные комнаты дома — с выходом, без острых предметов."),
            tr("Договоритесь с доверенным человеком о кодовом слове «мне нужна помощь»."),
            tr("Запомните наизусть 1–2 важных номера."),
            tr("Держите телефон заряженным, деньги — в доступном месте."),
            tr("Решите заранее, куда можно уйти: друзья, родственники, кризисный центр.")
        ), Icons.Filled.Shield),
    HelpCard(CardGroup.SAFETY, tr("Тревожный чемоданчик"), tr("Что собрать и где спрятать"),
        listOf(
            tr("Документы или их копии: паспорт, СНИЛС, полис, свидетельства, документы детей."),
            tr("Деньги, запасная банковская карта."),
            tr("Ключи, зарядка, лекарства на несколько дней."),
            tr("Смена одежды."),
            tr("Храните у доверенного человека или в надёжном месте вне дома."),
            tr("Сканы документов — в Сейфе приложения.")
        ), Icons.Filled.Shield),
    HelpCard(CardGroup.SAFETY, tr("Цифровая безопасность"), tr("Чтобы вас не отслеживали"),
        listOf(
            tr("Смените пароли от почты и соцсетей, включите двухфакторную защиту."),
            tr("Проверьте, нет ли на телефоне незнакомых приложений и доступа к геолокации у других."),
            tr("Выйдите из аккаунтов на чужих устройствах."),
            tr("Проверьте настройки «поделиться геопозицией»."),
            tr("Используйте PIN, который никто не знает.")
        ), Icons.Filled.Shield),

    // ---- Права до 18 лет (РФ). Не юридическая консультация. ----
    HelpCard(CardGroup.TEEN, tr("Твои права в семье"), tr("Что говорит Семейный кодекс"),
        listOf(
            tr("Родители обязаны воспитывать без пренебрежительного, жестокого, грубого и унижающего обращения, оскорблений и эксплуатации (ст. 65 СК РФ)."),
            tr("У тебя есть право на защиту от злоупотреблений со стороны родителей (ст. 56 СК РФ)."),
            tr("Можно самостоятельно обратиться в орган опеки и попечительства в любом возрасте, а с 14 лет — в суд (ст. 56 СК РФ)."),
            tr("С 10 лет твоё мнение обязаны учитывать при решениях, которые тебя касаются (ст. 57 СК РФ)."),
            tr("Это общая информация, а не юридическая консультация. Бесплатно помогут юридические клиники вузов и детский телефон доверия.")
        ), Icons.Filled.Gavel),
    HelpCard(CardGroup.TEEN, tr("Деньги и вещи"), tr("Экономическое давление"),
        listOf(
            tr("С 14 лет свой заработок, стипендию и другие доходы можно тратить самостоятельно (ст. 26 ГК РФ)."),
            tr("Родители обязаны содержать детей до 18 лет (ст. 80 СК РФ): еда, одежда, жильё, лечение, учёба."),
            tr("Если деньги или вещи постоянно отбирают в наказание или чтобы контролировать — это может быть формой насилия. Запиши такие случаи в Дневник.")
        ), Icons.Filled.Gavel),
    HelpCard(CardGroup.TEEN, tr("Переписка и телефон"), tr("Где забота, а где контроль"),
        listOf(
            tr("Тайна переписки и телефонных разговоров защищена Конституцией для всех, включая подростков (ст. 23)."),
            tr("Родители отвечают за твою безопасность, поэтому разговор о правилах пользования телефоном — нормален."),
            tr("Тайное чтение переписки, высмеивание её содержимого или постоянная слежка — уже контроль, а не забота."),
            tr("Включи блокировку экрана и не сообщай пароли. В приложении есть кнопка быстрого выхода.")
        ), Icons.Filled.Gavel),
    HelpCard(CardGroup.TEEN, tr("Медицинская и психологическая помощь"), tr("Когда можно обращаться самостоятельно"),
        listOf(
            tr("С 15 лет можно самостоятельно давать согласие на медицинскую помощь (ст. 54 ФЗ № 323 «Об основах охраны здоровья»)."),
            tr("Это включает приём у врача-психиатра и психотерапевта."),
            tr("Анонимно поговорить с психологом можно по детскому телефону доверия 8-800-2000-122 в любом возрасте.")
        ), Icons.Filled.Gavel),
    HelpCard(CardGroup.TEEN, tr("Если выгоняют из дома или опасно оставаться"), tr("Куда идти"),
        listOf(
            tr("При угрозе жизни — 112 или 102."),
            tr("Социально-реабилитационные центры для несовершеннолетних принимают в том числе по личному обращению самого подростка (ст. 13 ФЗ № 120)."),
            tr("Можно обратиться в орган опеки и попечительства своего района."),
            tr("Детский телефон доверия 8-800-2000-122 подскажет ближайший центр помощи."),
            tr("Можно пойти к взрослому, которому доверяешь: родственнику, учителю, школьному психологу.")
        ), Icons.Filled.Gavel),

    // ---- До 18: пошаговые алгоритмы ----
    HelpCard(CardGroup.TEEN, tr("Забрали паспорт или вещи"), tr("Что делать по шагам"),
        listOf(
            tr("Паспорт — твой личный документ. Удерживать чужой паспорт против воли его владельца закон не разрешает, даже родителям."),
            tr("Сначала спокойно попроси вернуть, лучше при свидетеле: «Верни, пожалуйста, мой паспорт, он мне нужен». Запиши в Дневник дату и ответ."),
            tr("Если не возвращают — можно подать заявление в полицию об удержании документа. Его обязаны принять и выдать талон-уведомление."),
            tr("Если паспорт не вернуть, обратись в МВД (вопросы миграции) или МФЦ: заяви об утрате и подай на новый. На время замены выдают временное удостоверение личности."),
            tr("Вещи, купленные на твой заработок или стипендию, с 14 лет — твои (ст. 26 ГК РФ). Сфотографируй чеки, если они есть, и сохрани в Сейф."),
            tr("Не знаешь, с чего начать, — позвони на детский телефон доверия 8-800-2000-122: там помогут составить план."),
        ), Icons.Filled.Gavel),
    HelpCard(CardGroup.TEEN, tr("Полиция не едет или встаёт на сторону родителей"), tr("Что говорить и что требовать"),
        listOf(
            tr("Звони 112 или 102. Говори коротко: адрес, что происходит прямо сейчас, есть ли побои или угрозы, сколько тебе лет."),
            tr("Фраза, которая работает: «Мне N лет, я несовершеннолетний человек, мне угрожает опасность дома, прошу направить наряд»."),
            tr("Спроси фамилию дежурного и номер, под которым зарегистрировано сообщение. Сообщение обязаны зарегистрировать. Запиши всё в Дневник."),
            tr("Если наряд приехал и не помогает — попроси принять твоё заявление и пригласить инспектора по делам несовершеннолетних (ПДН)."),
            tr("Есть побои — иди в травмпункт: повреждения зафиксируют в медицинских документах."),
            tr("Если в полиции отказали или ничего не сделали — можно пожаловаться в прокуратуру, в том числе онлайн через сайт прокуратуры. Приложи даты, фамилии и номер сообщения."),
        ), Icons.Filled.Gavel),
    HelpCard(CardGroup.TEEN, tr("Органы опеки бездействуют"), tr("Как добиться ответа"),
        listOf(
            tr("Обращайся письменно, а не только устно. Сделай копию и попроси поставить на ней отметку о приёме с датой."),
            tr("На письменное обращение обязаны ответить, обычно в течение 30 дней. Отметь дату в Дневнике."),
            tr("Если ответа нет или он формальный — пиши руководителю органа опеки, в комиссию по делам несовершеннолетних и защите их прав (КДН) при администрации района и в прокуратуру."),
            tr("Есть уполномоченный по правам ребёнка в твоём регионе — ему тоже можно написать, это бесплатно."),
            tr("Опиши факты: что, когда, кто видел. Приложи записи из Дневника и отчёт из приложения."),
            tr("Помочь составить обращение могут на детском телефоне доверия 8-800-2000-122 и в юридических клиниках вузов."),
        ), Icons.Filled.Gavel),

    // ---- Разрыв и синдром отмены ----
    HelpCard(CardGroup.ESCAPE, tr("Психологический манифест"), tr("Читать при вспышках вины"),
        listOf(
            tr("Мой уход — это не месть и не предательство. Это вынужденная самооборона."),
            tr("Я не обязан быть хорошим для тех, кто меня разрушал."),
            tr("Я не отвечаю за чувства, зависимости, бедность или одиночество взрослого человека, который причинял мне вред."),
            tr("Безопасность моей жизни важнее чужих манипуляций и одобрения."),
            tr("Тянет вернуться — это синдром отмены, а не любовь. Он пройдёт.")
        ), Icons.Filled.Shield),
    HelpCard(CardGroup.ESCAPE, tr("Метод «серого камня»"), tr("Если контакта не избежать"),
        listOf(
            tr("Станьте максимально скучным и предсказуемым собеседником. Агрессор питается эмоциями — без них он теряет интерес."),
            tr("Отвечайте тихо, монотонно, одним-двумя словами."),
            tr("Готовые фразы: «Да». «Нет». «Хорошо». «Я услышал(а)»."),
            tr("Не спорьте, не оправдывайтесь, не доказывайте правоту, не плачьте на виду."),
            tr("Ваши аргументы и эмоции могут использовать против вас. Берегите их.")
        ), Icons.Filled.Block),
    HelpCard(CardGroup.ESCAPE, tr("Откат: тянет вернуться"), tr("Когда страшно и одиноко"),
        listOf(
            tr("Заземление: умойтесь ледяной водой, подышите — вдох на 4, выдох на 7. Это гасит панику."),
            tr("Факты: перечитайте в Дневнике список причин, по которым вы ушли. Память стирает плохое — список вернёт к реальности."),
            tr("Импульс: если рука тянется разблокировать агрессора — займите тело на 15 минут (20 приседаний, мытьё посуды, быстрая прогулка)."),
            tr("Через 15 минут пик аффекта спадёт. Решения принимайте после этого.")
        ), Icons.Filled.Waves),

    // ---- Телефоны ----
    HelpCard(CardGroup.HELP, tr("Телефон доверия"), tr("Бесплатно, анонимно, круглосуточно"),
        listOf(tr("Можно звонить, даже если не знаете, что сказать.")),
        Icons.Filled.Call,
        phones = listOf(tr("Общероссийский телефон доверия") to "88002000122")),
)

@Composable
fun SupportScreen() {
    var group by rememberSaveable { mutableStateOf(CardGroup.CRISIS) }
    val cards = remember(group) { HELP_CARDS.filter { it.group == group } }

    ScreenScaffold(
        title = tr("Опора"),
        subtitle = tr("Техники для трудных минут. Выберите то, что подходит сейчас."),
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
                Text(tr("Отмечайте пункты — прогресс сохраняется."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp))
            }
            items(CHECKLISTS, key = { it.id }) { ChecklistCard(it) }
        }
        if (group == CardGroup.TESTS) {
            item {
                Text(
                    tr("Тесты помогают взглянуть со стороны. Это не диагноз, ответы нигде не сохраняются."),
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
                            Icon(Icons.Filled.Call, tr("Позвонить"))
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
