package com.neonplayer.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.neonplayer.domain.expansion.ThemeExpansion
import com.neonplayer.domain.model.AccentMode

/**
 * Expansions installed on this device, provided by [NeonTheme]. Empty when no add-on package
 * is present, which is the normal case for the free app.
 */
val LocalThemeExpansions = staticCompositionLocalOf { emptyList<ThemeExpansion>() }

/**
 * Resolves a stored accent id to a palette. Built-in accents win over expansions so an add-on
 * can never shadow Cyan or Magenta; an unknown or uninstalled id falls back to Cyan rather
 * than leaving the UI on a palette that no longer exists.
 */
fun paletteFor(accentId: String, expansions: List<ThemeExpansion>): NeonPalette {
    AccentMode.entries.firstOrNull { it.name.equals(accentId, ignoreCase = true) }
        ?.let { return paletteFor(it) }
    expansions.firstOrNull { it.accentId == accentId }?.let {
        return NeonPalette(
            primary = Color(it.primaryArgb),
            secondary = Color(it.secondaryArgb),
            tertiary = Color(it.tertiaryArgb),
        )
    }
    return paletteFor(AccentMode.CYAN)
}
