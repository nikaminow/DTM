package cloud.amnezia.dtm.ui

import cloud.amnezia.dtm.tr

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import cloud.amnezia.dtm.Recorder

enum class Route(val label: String, val icon: ImageVector, val selectedIcon: ImageVector, val inBar: Boolean = true) {
    HOME(tr("Главное"), Icons.Outlined.Home, Icons.Filled.Home),
    RECORD(tr("Запись"), Icons.Outlined.Mic, Icons.Filled.Mic),
    JOURNAL(tr("Дневник"), Icons.Outlined.Book, Icons.Filled.Book),
    VAULT(tr("Сейф"), Icons.Outlined.Lock, Icons.Filled.Lock),
    SUPPORT(tr("Опора"), Icons.Outlined.Spa, Icons.Filled.Spa),
    SOS("SOS", Icons.Filled.Shield, Icons.Filled.Shield, inBar = false),
    PROGRESS(tr("Прогресс"), Icons.Filled.Home, Icons.Filled.Home, inBar = false),
    DREAMS(tr("Сны"), Icons.Filled.Home, Icons.Filled.Home, inBar = false),
    MODES(tr("Режимы"), Icons.Filled.Home, Icons.Filled.Home, inBar = false),
    SMER(tr("СМЭР"), Icons.Filled.Home, Icons.Filled.Home, inBar = false),
    BREATH(tr("Дыхание"), Icons.Filled.Home, Icons.Filled.Home, inBar = false),
    CALENDAR(tr("Календарь"), Icons.Filled.Home, Icons.Filled.Home, inBar = false),
    AUDIOGUIDE(tr("Голосовой гид"), Icons.Filled.Home, Icons.Filled.Home, inBar = false),
    MEDITATION(tr("Медитации"), Icons.Filled.Home, Icons.Filled.Home, inBar = false),
    DBT(tr("Курс ДПТ"), Icons.Filled.Home, Icons.Filled.Home, inBar = false),
    SETTINGS(tr("Настройки"), Icons.Filled.Home, Icons.Filled.Home, inBar = false),
}

/** Одноразовые команды между экранами. */
object NavFlags {
    var openJournalEditor by mutableStateOf(false)
}

@Composable
fun DtmAppUi(requested: Route?, onRequestHandled: () -> Unit, onQuickExit: () -> Unit) {
    var route by rememberSaveable { mutableStateOf(Route.HOME) }
    var lastTab by rememberSaveable { mutableStateOf(Route.HOME) }
    fun go(r: Route) { if (r.inBar) lastTab = r; route = r }

    LaunchedEffect(requested) { requested?.let { go(it); onRequestHandled() } }
    BackHandler(enabled = route != Route.HOME) { route = if (route.inBar) Route.HOME else lastTab }

    val snack = remember { SnackbarHostState() }
    val cs = MaterialTheme.colorScheme

    CompositionLocalProvider(LocalSnackbar provides snack) {
      SoftBackground {
            Box(Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = route,
                    transitionSpec = {
                        (fadeIn(tween(320, easing = FastOutSlowInEasing)) +
                            scaleIn(tween(320, easing = FastOutSlowInEasing), initialScale = 0.985f)) togetherWith
                            fadeOut(tween(140))
                    },
                    label = "route",
                    modifier = Modifier.fillMaxSize(),
                ) { r ->
                    when (r) {
                        Route.HOME -> HomeScreen(go = ::go, onQuickExit = onQuickExit)
                        Route.RECORD -> RecordScreen()
                        Route.JOURNAL -> JournalScreen()
                        Route.VAULT -> VaultScreen()
                        Route.SUPPORT -> SupportScreen()
                        Route.SOS -> SosScreen(onBack = { route = lastTab }, openSettings = { go(Route.SETTINGS) })
                        Route.PROGRESS -> ProgressScreen(onBack = { route = lastTab })
                        Route.DREAMS -> DreamScreen(onBack = { route = lastTab })
                        Route.MODES -> ModesScreen(onBack = { route = lastTab })
                        Route.SMER -> SmerScreen(onBack = { route = lastTab })
                        Route.BREATH -> BreathScreen(onBack = { route = lastTab })
                        Route.CALENDAR -> CalendarScreen(onBack = { route = lastTab })
                        Route.AUDIOGUIDE -> AudioGuideScreen(onBack = { route = lastTab })
                        Route.MEDITATION -> MeditationScreen(onBack = { route = lastTab }, openVoice = { route = Route.AUDIOGUIDE })
                        Route.DBT -> DbtScreen(onBack = { route = lastTab })
                        Route.SETTINGS -> SettingsScreen(onBack = { route = lastTab })
                    }
                }

                if (route.inBar) Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    SosPill { go(Route.SOS) }
                    Spacer(Modifier.height(12.dp))
                    BottomBar(route) { go(it) }
                }

                UpdateAction(autoCheck = true)

                SnackbarHost(
                    snack,
                    Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = if (route.inBar) 170.dp else 16.dp),
                )
            }
      }
    }
}

/**
 * Нижняя панель: у всех вкладок одинаковая ширина, поэтому при переключении
 * ничего не сдвигается. Меняются только цвета — плавно.
 */
@Composable
private fun BottomBar(current: Route, onSelect: (Route) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(32.dp)
    Row(
        Modifier.fillMaxWidth().height(72.dp)
            .shadow(14.dp, shape, ambientColor = Color(0x26405048), spotColor = Color(0x26405048))
            .clip(shape)
            .background(cs.surfaceContainerLowest)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Route.entries.filter { it.inBar }.forEach { r ->
            val sel = current == r
            val pill by animateColorAsState(if (sel) cs.primaryContainer else Color.Transparent, tween(260), label = "pill")
            val tint by animateColorAsState(if (sel) cs.onPrimaryContainer else cs.onSurfaceVariant, tween(260), label = "tint")
            Column(
                Modifier.weight(1f).fillMaxHeight().softClickable { onSelect(r) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier.size(width = 54.dp, height = 32.dp).clip(RoundedCornerShape(16.dp)).background(pill),
                    contentAlignment = Alignment.Center,
                ) {
                    BadgedBox(badge = {
                        if (r == Route.RECORD && Recorder.isRecording) Badge(containerColor = cs.tertiary)
                    }) { Icon(if (sel) r.selectedIcon else r.icon, r.label, tint = tint, modifier = Modifier.size(22.dp)) }
                }
                Spacer(Modifier.height(3.dp))
                Text(r.label, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SosPill(onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)
    Row(
        Modifier.height(56.dp).softClickable(onClick = onClick)
            .shadow(10.dp, shape, ambientColor = Color(0x33704A38), spotColor = Color(0x33704A38))
            .clip(shape)
            .background(cs.tertiaryContainer)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Shield, null, tint = cs.onTertiaryContainer)
        Spacer(Modifier.width(8.dp))
        Text("SOS", style = MaterialTheme.typography.titleMedium, color = cs.onTertiaryContainer)
    }
}
