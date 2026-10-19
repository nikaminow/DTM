package cloud.amnezia.dtm.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import cloud.amnezia.dtm.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * Мягкая палитра DTM: шалфей (primary), приглушённый сланец (secondary),
 * тёплый песок/глина (tertiary — тревожные действия). Светлая тема — тёплая «бумага»,
 * тёмная — глубокий графит. Без ярких и стереотипных цветов.
 */
private val DarkScheme = darkColorScheme(
    primary = Color(0xFFC2DDCD), onPrimary = Color(0xFF1B3429),
    primaryContainer = Color(0xFF3A5547), onPrimaryContainer = Color(0xFFDDF1E5),
    secondary = Color(0xFFC6CFDA), onSecondary = Color(0xFF283341),
    secondaryContainer = Color(0xFF414B59), onSecondaryContainer = Color(0xFFE0E7F1),
    tertiary = Color(0xFFEDCDB6), onTertiary = Color(0xFF452B1D),
    tertiaryContainer = Color(0xFF6B4E40), onTertiaryContainer = Color(0xFFFDE6D7),
    error = Color(0xFFEDBBAA), onError = Color(0xFF4A2418),
    errorContainer = Color(0xFF6A3E31), onErrorContainer = Color(0xFFFFDDD2),
    background = Color(0xFF1B1D1C), onBackground = Color(0xFFEAE8E3),
    surface = Color(0xFF1B1D1C), onSurface = Color(0xFFEAE8E3),
    surfaceVariant = Color(0xFF3F4744), onSurfaceVariant = Color(0xFFBEC6C1),
    surfaceTint = Color(0xFFB7D3C4),
    outline = Color(0xFF878F8B), outlineVariant = Color(0xFF3B4240),
    inverseSurface = Color(0xFFE4E5E2), inverseOnSurface = Color(0xFF2E3132),
    inversePrimary = Color(0xFF4E6E5F),
    surfaceDim = Color(0xFF1B1D1C), surfaceBright = Color(0xFF414442),
    surfaceContainerLowest = Color(0xFF151716), surfaceContainerLow = Color(0xFF242725),
    surfaceContainer = Color(0xFF292C2A), surfaceContainerHigh = Color(0xFF313432),
    surfaceContainerHighest = Color(0xFF3A3D3B),
    scrim = Color(0xFF000000),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF4E6E5F), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD5EADD), onPrimaryContainer = Color(0xFF0D2A1E),
    secondary = Color(0xFF56657A), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE4EF), onSecondaryContainer = Color(0xFF131F2D),
    tertiary = Color(0xFF8A5E48), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF6DCCB), onTertiaryContainer = Color(0xFF34170A),
    error = Color(0xFF94503E), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFADCD2), onErrorContainer = Color(0xFF3A0E04),
    background = Color(0xFFF6F3EE), onBackground = Color(0xFF1D1F1E),
    surface = Color(0xFFF6F3EE), onSurface = Color(0xFF1D1F1E),
    surfaceVariant = Color(0xFFE1E4DD), onSurfaceVariant = Color(0xFF474D49),
    surfaceTint = Color(0xFF4E6E5F),
    outline = Color(0xFF777D79), outlineVariant = Color(0xFFCFD3CC),
    inverseSurface = Color(0xFF2F3130), inverseOnSurface = Color(0xFFF1EFEA),
    inversePrimary = Color(0xFFB7D3C4),
    surfaceDim = Color(0xFFDEDAD3), surfaceBright = Color(0xFFFBF9F5),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFBF9F5),
    surfaceContainer = Color(0xFFF0EDE7), surfaceContainerHigh = Color(0xFFEAE7E0),
    surfaceContainerHighest = Color(0xFFE4E1DA),
    scrim = Color(0xFF000000),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(40.dp),
)

/** Nunito — округлый дружелюбный шрифт с кириллицей (переменная толщина). */
@OptIn(ExperimentalTextApi::class)
private fun nunito(w: Int) = Font(
    R.font.nunito, FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w)),
)

@OptIn(ExperimentalTextApi::class)
val Nunito = FontFamily(nunito(400), nunito(500), nunito(600), nunito(700), nunito(800))

private val AppType = Typography().let { t ->
    fun TextStyle.n(w: Int, size: Int? = null, lh: Int? = null) = copy(
        fontFamily = Nunito, fontWeight = FontWeight(w), letterSpacing = 0.sp,
        fontSize = size?.sp ?: fontSize, lineHeight = lh?.sp ?: lineHeight,
    )
    t.copy(
        displayLarge = t.displayLarge.n(800), displayMedium = t.displayMedium.n(800), displaySmall = t.displaySmall.n(800),
        headlineLarge = t.headlineLarge.n(800, 32, 38), headlineMedium = t.headlineMedium.n(800, 28, 34),
        headlineSmall = t.headlineSmall.n(800, 22, 28),
        titleLarge = t.titleLarge.n(700, 20, 26), titleMedium = t.titleMedium.n(700, 17, 23), titleSmall = t.titleSmall.n(700, 14, 20),
        bodyLarge = t.bodyLarge.n(500, 16, 24), bodyMedium = t.bodyMedium.n(500, 15, 22), bodySmall = t.bodySmall.n(500, 13, 18),
        labelLarge = t.labelLarge.n(700, 15, 20), labelMedium = t.labelMedium.n(700, 13, 18), labelSmall = t.labelSmall.n(700, 11, 16),
    )
}

val TimerStyle = TextStyle(fontFamily = Nunito, fontSize = 52.sp, fontWeight = FontWeight(300), letterSpacing = 2.sp)

/** Настройки оформления. 0 — тёмная, 1 — как в системе, 2 — светлая. */
object UiPrefs {
    var themeMode by mutableIntStateOf(2)
    var dynamic by mutableStateOf(false)
}

@Composable
fun DtmTheme(content: @Composable () -> Unit) {
    val dark = when (UiPrefs.themeMode) {
        1 -> isSystemInDarkTheme()
        2 -> false
        else -> true
    }
    val context = LocalContext.current
    // Только собственная палитра DTM — без цветов обоев.
    val scheme: ColorScheme = if (dark) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = scheme, typography = AppType, shapes = AppShapes, content = content)
}

/**
 * Мягкий фон: базовый цвет поверхности и три крупных размытых пятна
 * (шалфей, сланец, песок). Рисуется один раз, без анимации — не тратит батарею.
 */
@Composable
fun SoftBackground(content: @Composable BoxScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    val a = cs.primaryContainer
    val b = cs.secondaryContainer
    val c = cs.tertiaryContainer
    Box(Modifier.fillMaxSize().background(cs.surface)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            fun blob(x: Float, y: Float, r: Float, color: Color, alpha: Float) = drawCircle(
                Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), Offset(x, y), r),
                radius = r, center = Offset(x, y),
            )
            blob(w * 0.05f, h * 0.06f, w * 1.05f, a, 0.65f)
            blob(w * 1.05f, h * 0.40f, w * 0.95f, b, 0.50f)
            blob(w * 0.15f, h * 0.92f, w * 1.0f, c, 0.45f)
        }
        content()
    }
}
