package cloud.amnezia.dtm.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Собственная дизайн-система DTM: мягкие карточки, кнопки-«пилюли», чипы.
 * Нажатие — лёгкое пружинистое «вдавливание» без ряби Material.
 */

/** Мягкое нажатие: элемент чуть уменьшается и пружинисто возвращается. */
fun Modifier.softClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && enabled) 0.965f else 1f,
        spring(dampingRatio = 0.55f, stiffness = 600f), label = "press",
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

private val CardShape = RoundedCornerShape(28.dp)

/** Мягкая карточка: скругление, еле заметная тень и обводка. */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .then(if (onClick != null) Modifier.softClickable(onClick = onClick) else Modifier)
            .shadow(3.dp, CardShape, ambientColor = Color(0x18405048), spotColor = Color(0x18405048))
            .clip(CardShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), CardShape)
            .padding(padding),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) { content() }
    }
}

enum class PillStyle { Primary, Soft, Warm, Ghost }

/** Кнопка-«пилюля». */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: PillStyle = PillStyle.Primary,
    enabled: Boolean = true,
) {
    val cs = MaterialTheme.colorScheme
    val (bg, fg) = when (style) {
        PillStyle.Primary -> cs.primary to cs.onPrimary
        PillStyle.Soft -> cs.primaryContainer to cs.onPrimaryContainer
        PillStyle.Warm -> cs.tertiaryContainer to cs.onTertiaryContainer
        PillStyle.Ghost -> cs.surfaceContainerHigh to cs.onSurface
    }
    Row(
        modifier
            .height(54.dp)
            .softClickable(enabled, onClick)
            .clip(RoundedCornerShape(27.dp))
            .background(if (enabled) bg else bg.copy(alpha = 0.45f))
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** Мягкий чип выбора. */
@Composable
fun SoftChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) cs.primary else cs.surfaceContainerHigh, label = "chipbg")
    val fg by animateColorAsState(if (selected) cs.onPrimary else cs.onSurface, label = "chipfg")
    Box(
        Modifier.softClickable(onClick = onClick)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) { Text(text, color = fg, style = MaterialTheme.typography.labelLarge) }
}

/** Круглая иконка на пастельной подложке. */
@Composable
fun SoftIcon(icon: ImageVector, bg: Color = MaterialTheme.colorScheme.secondaryContainer,
             tint: Color = MaterialTheme.colorScheme.onSecondaryContainer, size: Dp = 46.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** Информационная карточка с иконкой. */
@Composable
fun InfoCard(icon: ImageVector, title: String, text: String) {
    SoftCard(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
        Row {
            SoftIcon(icon, size = 40.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
