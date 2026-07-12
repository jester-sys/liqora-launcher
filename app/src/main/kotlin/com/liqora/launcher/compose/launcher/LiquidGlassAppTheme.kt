package com.liqora.launcher.compose.launcher



import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * =========================================================================
 *  BLUE LIQUID GLASS — App-wide design language (Apple VisionOS inspired)
 * =========================================================================
 * One color system, two moods (Dark / Light). Every screen's local token
 * objects (HomeTokens, SettingsTokens, ...) read from [GlassThemeState]
 * instead of hardcoding hex values, so the whole app stays in sync and
 * automatically follows the device's Dark/Light setting.
 *
 * Primary is always Apple Blue — this is the one constant across both
 * themes, since "Blue Liquid Glass" is the app's identity, not just a
 * dark-mode accent.
 *
 * This file also hosts the shared metrics system — [LiquidGlassSpacing],
 * [LiquidGlassRadius], [LiquidGlassTypography] and [LiquidGlassElevation] —
 * so every screen pulls padding, corner radii, type sizes and shadow/glow
 * recipes from one place instead of re-deriving magic numbers per file.
 * Existing per-screen token objects (HomeTokens, SettingsTokens, ...) are
 * untouched and can adopt these incrementally.
 */
data class LiquidGlassColors(
    val primary: Color,          // Apple Blue — brand color, glass tint, buttons, active states
    val primaryVariant: Color,   // Deeper blue — gradient endpoint, pressed states
    val background: Color,       // Screen background (behind blur/backdrop layers)
    val backgroundElevated: Color, // Secondary background wash, e.g. bottom of a screen gradient
    val surface: Color,          // Opaque surface fallback (glass disabled / low-power mode)
    val glassSurface: Color,     // Translucent blue-tinted fill used for cards/dialogs/sheets/bars
    val glassSurfaceElevated: Color, // Stronger tint for modals/sheets that sit above other glass
    val glassBorder: Color,      // Thin edge border that sells the "glass pane" look
    val glassHighlight: Color,   // Specular highlight, e.g. top edge of a glass sheet
    val glassGlow: Color,        // Soft outer blue glow behind elevated glass elements
    val divider: Color,          // Hairline separators inside glass surfaces
    val accent: Color,           // Secondary accent (success / positive state)
    val warning: Color,
    val error: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,     // Faint captions / placeholder-level text
    val iconOnGlass: Color,      // Icon tint sitting directly on glass surfaces
    val scrim: Color             // Backdrop dimming behind dialogs/sheets
)

val DarkLiquidGlassColors = LiquidGlassColors(
    primary = Color(0xFF0A84FF),
    primaryVariant = Color(0xFF0040DD),
    background = Color(0xFF0F1115),
    backgroundElevated = Color(0xFF08080C),
    surface = Color(0xFF1C1C1E),
    glassSurface = Color(0xFF0A84FF).copy(alpha = 0.14f),
    glassSurfaceElevated = Color(0xFF1B2436).copy(alpha = 0.82f),
    glassBorder = Color.White.copy(alpha = 0.14f),
    glassHighlight = Color.White.copy(alpha = 0.30f),
    glassGlow = Color(0xFF0A84FF).copy(alpha = 0.35f),
    divider = Color.White.copy(alpha = 0.08f),
    accent = Color(0xFF30D158),
    warning = Color(0xFFFF9F0A),
    error = Color(0xFFFF453A),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFA1A1AA),
    textTertiary = Color(0xFF6E6E76),
    iconOnGlass = Color(0xFFEAF3FF),
    scrim = Color.Black.copy(alpha = 0.55f)
)

val LightLiquidGlassColors = LiquidGlassColors(
    primary = Color(0xFF0A84FF),
    primaryVariant = Color(0xFF1B5FE0),
    background = Color(0xFFEEF2F9),
    backgroundElevated = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    glassSurface = Color(0xFF0A84FF).copy(alpha = 0.10f),
    glassSurfaceElevated = Color.White.copy(alpha = 0.86f),
    glassBorder = Color(0xFF0A84FF).copy(alpha = 0.18f),
    glassHighlight = Color.White.copy(alpha = 0.70f),
    glassGlow = Color(0xFF0A84FF).copy(alpha = 0.22f),
    divider = Color(0xFF0C0D10).copy(alpha = 0.07f),
    accent = Color(0xFF248A3D),
    warning = Color(0xFFC46A00),
    error = Color(0xFFD70015),
    textPrimary = Color(0xFF0C0D10),
    textSecondary = Color(0xFF6B7280),
    textTertiary = Color(0xFF9AA1AC),
    iconOnGlass = Color(0xFF0A2647),
    scrim = Color.Black.copy(alpha = 0.32f)
)

/**
 * 4pt spacing scale used app-wide for padding, gaps and margins so rhythm
 * stays consistent across screens instead of ad-hoc dp values.
 */
object LiquidGlassSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

/** Corner radius scale — larger radii read as "softer glass", used for bigger surfaces. */
object LiquidGlassRadius {
    val sm = 12.dp
    val md = 16.dp
    val lg = 22.dp
    val xl = 28.dp
    val pill = 999.dp

    val shapeSm = RoundedCornerShape(sm)
    val shapeMd = RoundedCornerShape(md)
    val shapeLg = RoundedCornerShape(lg)
    val shapeXl = RoundedCornerShape(xl)
    val shapePill = RoundedCornerShape(pill)
}

/**
 * Apple-style type scale (SF-inspired sizing/tracking) with rounded weights
 * that read well against blurred glass. Colors are applied at call-sites via
 * `.copy(color = ...)` so the same scale works for both themes.
 */
object LiquidGlassTypography {
    val largeTitle = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
    val title1 = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp)
    val title2 = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp)
    val title3 = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    val headline = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal)
    val callout = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium)
    val subheadline = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal)
    val footnote = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp)
    val caption = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp)
}

/**
 * Reusable shadow/glow recipes for elevated glass. Two-layer approach:
 * a soft blue [glow] behind the surface for "premium glow" plus a tighter
 * contact [shadow] for physical grounding.
 */
object LiquidGlassElevation {
    data class Level(val glowRadius: Dp, val glowAlpha: Float, val shadowElevation: Dp)

    val resting = Level(glowRadius = 18.dp, glowAlpha = 0.18f, shadowElevation = 4.dp)
    val raised = Level(glowRadius = 28.dp, glowAlpha = 0.26f, shadowElevation = 10.dp)
    val floating = Level(glowRadius = 40.dp, glowAlpha = 0.34f, shadowElevation = 18.dp)
}

/** Premium blue gradient brushes derived from the active theme's colors. */
object LiquidGlassGradients {
    fun primaryGradient(colors: LiquidGlassColors) = Brush.linearGradient(
        listOf(colors.primary, colors.primaryVariant)
    )

    fun glassSheen(colors: LiquidGlassColors) = Brush.verticalGradient(
        listOf(colors.glassHighlight, Color.Transparent)
    )

    fun backgroundWash(colors: LiquidGlassColors) = Brush.verticalGradient(
        listOf(colors.background, colors.backgroundElevated)
    )
}

/**
 * Lightweight global holder for the active theme. Screens that were built
 * around a static "Tokens" object (HomeTokens, SettingsTokens) can point
 * their properties at [GlassThemeState.colors] with zero call-site changes.
 * Set once per composition pass by [LiquidGlassAppTheme].
 */
object GlassThemeState {
    var isDark: Boolean by mutableStateOf(true)
        internal set

    val colors: LiquidGlassColors
        get() = if (isDark) DarkLiquidGlassColors else LightLiquidGlassColors
}

/**
 * Root theme wrapper. Wrap the entire app content in this once (in the
 * launcher Activity) so every screen shares the same Blue Liquid Glass
 * language and reacts to system Dark/Light automatically.
 */
@Composable
fun LiquidGlassAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    GlassThemeState.isDark = darkTheme
    val colors = GlassThemeState.colors

    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = Color.White,
            secondary = colors.accent,
            onSecondary = Color.White,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            error = colors.error
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = Color.White,
            secondary = colors.accent,
            onSecondary = Color.White,
            background = colors.background,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            error = colors.error
        )
    }

    MaterialTheme(colorScheme = scheme, content = content)
}
