package com.neonplayer.domain.expansion

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Immutable

/**
 * A theme add-on that is bolted on as a separate installed package, not shipped inside the
 * base app. The base app contains no premium colours, backdrops or strings: if no expansion
 * package is installed, [ExpansionSource.installed] returns an empty list and the UI offers
 * only the built-in accents.
 *
 * See docs/premium-theme.md before changing this contract.
 */
@Immutable
data class ThemeExpansion(
    val packageName: String,
    val accentId: String,
    val label: String,
    val primaryArgb: Long,
    val secondaryArgb: Long,
    val tertiaryArgb: Long,
    val backdropAssets: List<String> = emptyList(),
)

interface ExpansionSource {
    fun installed(): List<ThemeExpansion>
}

/**
 * Discovers installed expansion packages by package-name prefix and reads their declared
 * accent from manifest meta-data. No network, no licence check, no verification: if the
 * package is installed, the theme is available. That is deliberate — a customer who has the
 * add-on keeps it across updates, reinstalls and device moves.
 */
class PackageExpansionSource(private val context: Context) : ExpansionSource {

    override fun installed(): List<ThemeExpansion> {
        val pm = context.packageManager
        return runCatching {
            pm.getInstalledPackages(0)
                .asSequence()
                .map { it.packageName }
                .filter { it.startsWith(EXPANSION_PREFIX) }
                .mapNotNull { read(pm, it) }
                .toList()
        }.getOrDefault(emptyList())
    }

    private fun read(pm: PackageManager, pkg: String): ThemeExpansion? = runCatching {
        val meta = pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA).metaData
            ?: return null
        val id = meta.getString(META_ACCENT_ID) ?: return null
        ThemeExpansion(
            packageName = pkg,
            accentId = id,
            label = meta.getString(META_LABEL) ?: id,
            primaryArgb = meta.getInt(META_PRIMARY, 0).toLong() and 0xFFFFFFFFL,
            secondaryArgb = meta.getInt(META_SECONDARY, 0).toLong() and 0xFFFFFFFFL,
            tertiaryArgb = meta.getInt(META_TERTIARY, 0).toLong() and 0xFFFFFFFFL,
            backdropAssets = meta.getString(META_BACKDROPS)
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList(),
        )
    }.getOrNull()

    companion object {
        const val EXPANSION_PREFIX = "com.neonplayer.expansion."
        const val META_ACCENT_ID = "neonplayer.accent.id"
        const val META_LABEL = "neonplayer.accent.label"
        const val META_PRIMARY = "neonplayer.accent.primary"
        const val META_SECONDARY = "neonplayer.accent.secondary"
        const val META_TERTIARY = "neonplayer.accent.tertiary"
        const val META_BACKDROPS = "neonplayer.backdrops"
    }
}
