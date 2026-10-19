@file:OptIn(ExperimentalMaterial3Api::class)

package cloud.amnezia.dtm.ui

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cloud.amnezia.dtm.Recorder
import cloud.amnezia.dtm.RecorderService
import cloud.amnezia.dtm.data.Contacts
import cloud.amnezia.dtm.data.Journal
import cloud.amnezia.dtm.data.MoodEntry
import cloud.amnezia.dtm.data.SecureStore
import cloud.amnezia.dtm.data.Tracker
import cloud.amnezia.dtm.data.Vault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Блоки главного экрана — их можно скрывать и переставлять. */
enum class HomeBlock(val title: String) {
    MOOD("Как вы сейчас"),
    WARM("Тёплые слова"),
    SOS("Сигнал тревоги"),
    QUICK("Быстрые действия"),
    TOOLS("Дневники и практики"),
    REALITY("Карта реальности"),
    STATS("Счётчики"),
    INFO("Ваша реальность имеет значение"),
    TIPS("Подсказка о быстром доступе"),
}

object HomeLayout {
    private const val KEY = "home_blocks"

    fun load(context: Context): List<Pair<HomeBlock, Boolean>> {
        val raw = SecureStore.getString(context, KEY).orEmpty()
        val saved = raw.split(',').mapNotNull { part ->
            val name = part.substringBefore(':')
            val b = HomeBlock.entries.firstOrNull { it.name == name } ?: return@mapNotNull null
            b to (part.substringAfter(':', "1") == "1")
        }
        // Новые блоки, которых ещё нет в сохранённой раскладке, добавляем в конец.
        return saved + HomeBlock.entries.filter { b -> saved.none { it.first == b } }.map { it to true }
    }

    fun save(context: Context, list: List<Pair<HomeBlock, Boolean>>) {
        SecureStore.putString(context, KEY, list.joinToString(",") { "${it.first.name}:${if (it.second) 1 else 0}" })
    }
}

@Composable
fun HomeScreen(go: (Route) -> Unit, onQuickExit: () -> Unit) {
    val context = LocalContext.current
    val snack = LocalSnackbar.current
    val scope = rememberCoroutineScope()

    var recCount by remember { mutableIntStateOf(0) }
    var jourCount by remember { mutableIntStateOf(0) }
    var vaultCount by remember { mutableIntStateOf(0) }
    var contactCount by remember { mutableIntStateOf(0) }
    var lastMood by remember { mutableStateOf<MoodEntry?>(null) }
    var layout by remember { mutableStateOf(HomeLayout.load(context)) }
    var customize by remember { mutableStateOf(false) }
    val nick = remember { SecureStore.nick(context) }

    LaunchedEffect(Recorder.savedTick) {
        recCount = withContext(Dispatchers.IO) { Recorder.count(context) }
        jourCount = withContext(Dispatchers.IO) { Journal.count(context) }
        vaultCount = withContext(Dispatchers.IO) { Vault.count(context) }
        contactCount = withContext(Dispatchers.IO) { Contacts.load(context).size }
        lastMood = Tracker.moods(context).lastOrNull()
    }

    val recLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res[Manifest.permission.RECORD_AUDIO] == true) RecorderService.start(context)
        else scope.launch { snack.showSnackbar("Нужен доступ к микрофону") }
    }
    val startRecording = {
        if (Recorder.isRecording) go(Route.RECORD)
        else recLauncher.launch(
            if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
            else arrayOf(Manifest.permission.RECORD_AUDIO)
        )
    }

    ScreenScaffold(
        title = if (nick.isBlank()) "Привет 👋" else "Привет, $nick 👋",
        subtitle = "Здесь всё под вашим контролем. Ничего не покидает устройство без вашего решения.",
        actions = {
            HeaderIcon(Icons.Filled.Tune, "Настроить главный экран") { customize = true }
            HeaderIcon(Icons.AutoMirrored.Filled.ExitToApp, "Быстрый выход", onQuickExit)
            HeaderIcon(Icons.Filled.Settings, "Настройки") { go(Route.SETTINGS) }
        },
    ) {
        if (Recorder.isRecording) item(key = "rec") { RecordingBanner { RecorderService.stop(context) } }

        layout.filter { it.second }.forEach { (block, _) ->
            item(key = block.name) {
                when (block) {
                    HomeBlock.MOOD -> MoodCard(lastMood) { v ->
                        scope.launch {
                            Tracker.addMood(context, v)
                            lastMood = Tracker.moods(context).lastOrNull()
                        }
                    }
                    HomeBlock.WARM -> {
                        var i by remember { mutableIntStateOf(WARM_WORDS.indices.random()) }
                        SoftCard(
                            Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = { i = (i + 1) % WARM_WORDS.size },
                        ) {
                            Text("💚 ${WARM_WORDS[i]}", style = MaterialTheme.typography.titleMedium)
                            Text("Нажмите, чтобы увидеть ещё", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        }
                    }
                    HomeBlock.SOS -> SoftCard(
                        Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = { go(Route.SOS) }, padding = 20.dp,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SoftIcon(Icons.Filled.Shield, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary, 52.dp)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Сигнал тревоги", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    if (contactCount > 0) "$contactCount ${plural(contactCount, "доверенный контакт", "доверенных контакта", "доверенных контактов")}"
                                    else "Добавьте доверенный контакт",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                    HomeBlock.QUICK -> Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PillButton("Записать", { startRecording() }, Modifier.weight(1f), Icons.Filled.Mic, PillStyle.Soft)
                        PillButton("Событие", { NavFlags.openJournalEditor = true; go(Route.JOURNAL) }, Modifier.weight(1f),
                            Icons.AutoMirrored.Filled.NoteAdd, PillStyle.Soft)
                    }
                    HomeBlock.TOOLS -> Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NavTile("Прогресс", "график и практики", Icons.Outlined.ShowChart, Modifier.weight(1f)) { go(Route.PROGRESS) }
                            NavTile("Сны", "дневник снов", Icons.Outlined.Bedtime, Modifier.weight(1f)) { go(Route.DREAMS) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NavTile("Режимы", "дневник режимов", Icons.Outlined.Groups, Modifier.weight(1f)) { go(Route.MODES) }
                            NavTile("СМЭР", "мысли → эмоции", Icons.Outlined.Psychology, Modifier.weight(1f)) { go(Route.SMER) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NavTile("Дыхание", "успокоиться за минуту", Icons.Outlined.Air, Modifier.weight(1f)) { go(Route.BREATH) }
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    HomeBlock.REALITY -> RealityCard()
                    HomeBlock.STATS -> Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("$recCount", plural(recCount, "запись", "записи", "записей"), Modifier.weight(1f)) { go(Route.RECORD) }
                        StatCard("$jourCount", plural(jourCount, "событие", "события", "событий"), Modifier.weight(1f)) { go(Route.JOURNAL) }
                        StatCard("$vaultCount", plural(vaultCount, "документ", "документа", "документов"), Modifier.weight(1f)) { go(Route.VAULT) }
                    }
                    HomeBlock.INFO -> InfoCard(
                        Icons.Outlined.Visibility, "Ваша реальность имеет значение",
                        "Записи, события и документы хранятся зашифрованно, с датой и цифровым отпечатком. " +
                            "Если кто-то убеждает, что «этого не было», — у вас есть опора.",
                    )
                    HomeBlock.TIPS -> InfoCard(
                        Icons.Outlined.Lightbulb, "Быстрый доступ",
                        "Удерживайте значок приложения на рабочем столе: «Запись» начнёт запись сразу. " +
                            "Кнопка ⇥ вверху мгновенно закрывает приложение и убирает его из недавних.",
                    )
                }
            }
        }
    }

    if (customize) HomeCustomizer(
        initial = layout,
        onDone = { l -> layout = l; HomeLayout.save(context, l); customize = false },
    )
}

@Composable
fun HeaderIcon(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier.padding(start = 6.dp).size(44.dp).softClickable(onClick = onClick)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp)) }
}

/** Отметка состояния — доступна в любой момент, сколько угодно раз. */
@Composable
private fun MoodCard(last: MoodEntry?, onPick: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    SoftCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), color = cs.secondaryContainer,
        contentColor = cs.onSecondaryContainer) {
        Text("Как вы сейчас?", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MoodButton("🙁", "Плохо", last?.value == 1, cs.error, Modifier.weight(1f)) { onPick(1) }
            MoodButton("😐", "Средне", last?.value == 2, cs.tertiary, Modifier.weight(1f)) { onPick(2) }
            MoodButton("🙂", "Отлично", last?.value == 3, cs.primary, Modifier.weight(1f)) { onPick(3) }
        }
        AnimatedVisibility(last != null, enter = fadeIn() + expandVertically()) {
            val time = last?.let { SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(Date(it.time)) }.orEmpty()
            Text(
                "Последняя отметка: ${last?.let { Tracker.moodWord(it.value) }.orEmpty()} · $time. Можно отмечать сколько угодно раз.",
                style = MaterialTheme.typography.bodySmall, color = cs.onSecondaryContainer.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun MoodButton(emoji: String, label: String, selected: Boolean, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) color.copy(alpha = 0.18f) else cs.surfaceContainerLowest, label = "mood")
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.height(80.dp).softClickable(onClick = onClick).clip(shape).background(bg)
            .border(if (selected) 2.dp else 0.dp, if (selected) color else Color.Transparent, shape),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) color else cs.onSurfaceVariant)
    }
}

@Composable
private fun NavTile(title: String, hint: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    SoftCard(modifier, onClick = onClick, padding = 16.dp) {
        SoftIcon(icon, size = 40.dp)
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    SoftCard(modifier, onClick = onClick, padding = 16.dp) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun RecordingBanner(onStop: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var elapsed by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) { elapsed = (System.currentTimeMillis() - Recorder.startedAtMs) / 1000; delay(500) }
    }
    SoftCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), color = cs.primaryContainer,
        contentColor = cs.onPrimaryContainer, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SoftIcon(Icons.Filled.Mic, cs.primary, cs.onPrimary, 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Идёт запись", style = MaterialTheme.typography.titleMedium)
                Text(formatDuration(elapsed), style = MaterialTheme.typography.bodyMedium)
            }
            PillButton("Стоп", onStop, icon = Icons.Filled.Stop, style = PillStyle.Primary)
        }
    }
}

/** Настройка главного экрана: показать/скрыть блок и поменять порядок. */
@Composable
private fun HomeCustomizer(initial: List<Pair<HomeBlock, Boolean>>, onDone: (List<Pair<HomeBlock, Boolean>>) -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val items = remember { mutableStateListOf<Pair<HomeBlock, Boolean>>().apply { addAll(initial) } }
    val cs = MaterialTheme.colorScheme

    ModalBottomSheet(onDismissRequest = { onDone(items.toList()) }, sheetState = sheet,
        containerColor = cs.surfaceContainerLowest) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Главный экран", style = MaterialTheme.typography.headlineSmall)
            Text("Включайте нужные блоки и меняйте порядок стрелками.",
                style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            items.forEachIndexed { i, (block, on) ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cs.surfaceContainerLow)
                        .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(block.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f),
                        color = if (on) cs.onSurface else cs.onSurfaceVariant)
                    ArrowBtn(Icons.Filled.KeyboardArrowUp, "Выше", i > 0) {
                        val x = items.removeAt(i); items.add(i - 1, x)
                    }
                    ArrowBtn(Icons.Filled.KeyboardArrowDown, "Ниже", i < items.lastIndex) {
                        val x = items.removeAt(i); items.add(i + 1, x)
                    }
                    Switch(checked = on, onCheckedChange = { items[i] = block to it })
                }
            }
            Spacer(Modifier.height(8.dp))
            PillButton("Готово", { onDone(items.toList()) }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ArrowBtn(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).softClickable(enabled, onClick), contentAlignment = Alignment.Center) {
        Icon(icon, description, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.3f))
    }
}
