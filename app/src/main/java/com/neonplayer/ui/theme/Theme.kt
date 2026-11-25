package com.neonplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neonplayer.R
import com.neonplayer.domain.expansion.ThemeExpansion
import com.neonplayer.domain.model.AccentMode

object NeonColors {
    val Base0 = Color(0xFF05070D)
    val Base1 = Color(0xFF0A0E1A)
    val Surface = Color(0xFF0C1222)
    val SurfaceHigh = Color(0xFF111A2E)
    val Cyan = Color(0xFF22E5FF)
    val Magenta = Color(0xFFFF2BD6)
    val Pink = Color(0xFFFF3D8F)
    val Maroon = Color(0xFF2A0716)
    val TextPrimary = Color(0xFFE6F7FF)
    val TextSecondary = Color(0xFF9FB3C8)
    val TextMuted = Color(0xFF5E7088)
    val Grid = Color(0xFF22E5FF).copy(alpha = 0.05f)
    val Danger = Color(0xFFFF5470)
    val Success = Color(0xFF3DFFA8)
}

@Immutable
data class NeonPalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val textPrimary: Color = NeonColors.TextPrimary,
    val textSecondary: Color = NeonColors.TextSecondary,
    val textMuted: Color = NeonColors.TextMuted,
    val base0: Color = NeonColors.Base0,
    val base1: Color = NeonColors.Base1,
    val surface: Color = NeonColors.Surface,
    val surfaceHigh: Color = NeonColors.SurfaceHigh,
) {
    val border: Color get() = primary.copy(alpha = 0.22f)
    val glow: Color get() = primary.copy(alpha = 0.18f)
}

fun paletteFor(accent: AccentMode): NeonPalette = when (accent) {
    AccentMode.CYAN -> NeonPalette(primary = NeonColors.Cyan, secondary = NeonColors.Magenta, tertiary = NeonColors.Pink)
    AccentMode.MAGENTA -> NeonPalette(primary = NeonColors.Magenta, secondary = NeonColors.Cyan, tertiary = NeonColors.Pink)
}

@Immutable
data class NeonStyle(
    val glassIcons: Boolean = true,
    val reduceMotion: Boolean = false,
)

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int, weight: Int) = Font(
    resId = res,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val GeometricSans = FontFamily(
    variable(R.font.space_grotesk, 400),
    variable(R.font.space_grotesk, 500),
    variable(R.font.space_grotesk, 600),
    variable(R.font.space_grotesk, 700),
)

val Mono = FontFamily(
    variable(R.font.jetbrains_mono, 400),
    variable(R.font.jetbrains_mono, 500),
    variable(R.font.jetbrains_mono, 700),
)

object NeonType {
    val Label = TextStyle(fontFamily = GeometricSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.4.sp)
    val TitleXL = TextStyle(fontFamily = GeometricSans, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = 0.2.sp)
    val TitleL = TextStyle(fontFamily = GeometricSans, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    val TitleM = TextStyle(fontFamily = GeometricSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    val Body = TextStyle(fontFamily = GeometricSans, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    val BodySmall = TextStyle(fontFamily = GeometricSans, fontWeight = FontWeight.Normal, fontSize = 12.sp)
    val MonoSmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.6.sp)
    val MonoBody = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 13.sp)
}

object NeonShapes {
    val CardRadius = 16.dp
    val SmallRadius = 10.dp
}

val LocalNeonPalette = staticCompositionLocalOf { paletteFor(AccentMode.CYAN) }
val LocalNeonStyle = staticCompositionLocalOf { NeonStyle() }

object Neon {
    val palette: NeonPalette @Composable get() = LocalNeonPalette.current
    val style: NeonStyle @Composable get() = LocalNeonStyle.current
}

@Composable
fun NeonTheme(
    accentId: String = AccentMode.CYAN.name,
    expansions: List<ThemeExpansion> = emptyList(),
    glassIcons: Boolean = true,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = remember(accentId, expansions) { paletteFor(accentId, expansions) }
    val style = remember(glassIcons, reduceMotion) { NeonStyle(glassIcons, reduceMotion) }
    val scheme = remember(palette) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.base0,
            secondary = palette.secondary,
            onSecondary = palette.base0,
            tertiary = palette.tertiary,
            background = palette.base0,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceHigh,
            onSurfaceVariant = palette.textSecondary,
            surfaceContainer = palette.surface,
            surfaceContainerHigh = palette.surfaceHigh,
            surfaceContainerHighest = palette.surfaceHigh,
            outline = palette.border,
            error = NeonColors.Danger,
        )
    }
    CompositionLocalProvider(LocalNeonPalette provides palette, LocalNeonStyle provides style) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
