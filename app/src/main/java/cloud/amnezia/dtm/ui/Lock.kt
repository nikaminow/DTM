package cloud.amnezia.dtm.ui

import cloud.amnezia.dtm.tr

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import cloud.amnezia.dtm.R
import cloud.amnezia.dtm.data.SecureStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LockScreen(
    bioAvailable: Boolean,
    onUnlocked: () -> Unit,
    onDuress: () -> Unit,
    onRequestBiometric: () -> Unit,
    onRestored: () -> Unit = {},
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val shake = remember { Animatable(0f) }
    val cs = MaterialTheme.colorScheme

    val setupMode = remember { !SecureStore.hasPin(context) }
    val len = remember { if (setupMode) 6 else SecureStore.pinLength(context) }
    var pin by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorTick by remember { mutableIntStateOf(0) }
    var checking by remember { mutableStateOf(false) }
    var lockedUntil by remember { mutableLongStateOf(SecureStore.lockedUntil(context)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val locked = lockedUntil > now

    LaunchedEffect(Unit) { if (bioAvailable) onRequestBiometric() }
    LaunchedEffect(lockedUntil) {
        while (lockedUntil > System.currentTimeMillis()) { now = System.currentTimeMillis(); delay(500) }
        now = System.currentTimeMillis()
    }
    LaunchedEffect(errorTick) {
        if (errorTick > 0) for (x in listOf(20f, -18f, 12f, -8f, 4f, 0f)) shake.animateTo(x, spring(stiffness = 5000f))
    }

    fun fail(text: String) {
        message = text; isError = true; pin = ""; errorTick++
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun submit(full: String) {
        if (setupMode) {
            val f = firstPin
            when {
                f == null -> { firstPin = full; pin = ""; message = ""; isError = false }
                f == full -> scope.launch {
                    checking = true
                    withContext(Dispatchers.Default) { SecureStore.setPin(context, full) }
                    onUnlocked()
                }
                else -> { firstPin = null; fail(tr("PIN-коды не совпали. Попробуйте ещё раз")) }
            }
        } else scope.launch {
            checking = true
            val res = withContext(Dispatchers.Default) { SecureStore.verify(context, full) }
            checking = false
            when (res) {
                1 -> onUnlocked()
                2 -> onDuress()
                else -> {
                    lockedUntil = SecureStore.lockedUntil(context)
                    fail(if (lockedUntil > System.currentTimeMillis()) tr("Слишком много попыток") else tr("Неверный PIN-код"))
                }
            }
        }
    }

    val title = when {
        setupMode && firstPin == null -> tr("Придумайте PIN-код")
        setupMode -> tr("Повторите PIN-код")
        else -> tr("Введите PIN-код")
    }
    val hint = when {
        locked -> tr("Повторить можно через %1\$s с", ((lockedUntil - now) / 1000) + 1)
        message.isNotEmpty() -> message
        setupMode && firstPin == null -> tr("6 цифр — их будете знать только вы")
        else -> ""
    }

    SoftBackground {
    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.6f))
        Image(painterResource(R.drawable.logo_mark), null, Modifier.size(88.dp).clip(CircleShape))
        Spacer(Modifier.height(16.dp))
        Text("DTM", style = MaterialTheme.typography.displaySmall, color = cs.onSurface)
        Text(tr("пространство, где вы в безопасности"), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(36.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
        Spacer(Modifier.height(20.dp))

        Row(Modifier.offset { IntOffset(shake.value.toInt(), 0) }, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(len) { i ->
                val filled = i < pin.length
                val c by animateColorAsState(
                    when { isError && pin.isEmpty() -> cs.error; filled -> cs.primary; else -> cs.surfaceContainerHighest },
                    label = "dot",
                )
                val s by animateDpAsState(if (filled) 16.dp else 12.dp, label = "dotSize")
                Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(s).clip(CircleShape).background(c))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            hint.ifEmpty { " " },
            color = if (isError || locked) cs.error else cs.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
        )
        // После переустановки можно сразу вернуть данные из резервной копии.
        if (setupMode && firstPin == null) {
            RestoreAction(onRestored = onRestored) { start ->
                androidx.compose.material3.TextButton(onClick = start) { Text(tr("Восстановить из резервной копии")) }
            }
        }
        Spacer(Modifier.weight(0.4f))

        Keypad(
            enabled = !locked && !checking,
            onDigit = { d ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (pin.length < len) {
                    isError = false
                    pin += d
                    if (pin.length == len) submit(pin)
                }
            },
            onDelete = { pin = pin.dropLast(1) },
            onBiometric = if (bioAvailable) onRequestBiometric else null,
        )
        Spacer(Modifier.height(24.dp))
    }
    }
}

@Composable
private fun Keypad(enabled: Boolean, onDigit: (Char) -> Unit, onDelete: () -> Unit, onBiometric: (() -> Unit)?) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        listOf("123", "456", "789").forEach { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                r.forEach { d -> Key(enabled, onClick = { onDigit(d) }) { Text("$d", style = MaterialTheme.typography.headlineMedium) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            if (onBiometric != null) Key(true, filled = false, onClick = onBiometric) {
                Icon(Icons.Filled.Fingerprint, tr("Вход по отпечатку"), tint = cs.primary, modifier = Modifier.size(32.dp))
            } else Spacer(Modifier.size(78.dp))
            Key(enabled, onClick = { onDigit('0') }) { Text("0", style = MaterialTheme.typography.headlineMedium) }
            Key(enabled, filled = false, onClick = onDelete) {
                Icon(Icons.AutoMirrored.Outlined.Backspace, tr("Стереть"), tint = cs.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Key(enabled: Boolean, filled: Boolean = true, onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (filled) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f) else androidx.compose.ui.graphics.Color.Transparent,
        shadowElevation = if (filled) 1.dp else 0.dp,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(78.dp),
    ) { Box(contentAlignment = Alignment.Center) { content() } }
}
