package com.neonplayer.data.settings

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * App-level language override. The tag is mirrored into SharedPreferences so
 * [wrap] can apply it synchronously from Activity.attachBaseContext, before any
 * resource is resolved. An empty tag means "follow the system language".
 */
object AppLocale {

    private const val PREFS = "neon_locale"
    private const val KEY = "tag"

    fun stored(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()

    /** Returns true when the stored tag actually changed. */
    fun store(context: Context, tag: String): Boolean {
        if (stored(context) == tag) return false
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, tag)
            .apply()
        return true
    }

    fun wrap(base: Context): Context {
        val tag = stored(base)
        val systemLocale = base.resources.configuration.locales[0] ?: Locale.getDefault()
        if (tag.isBlank()) {
            Locale.setDefault(systemLocale)
            return base
        }
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    /** A context whose resources follow the stored app language, for non-Activity callers. */
    fun localized(context: Context): Context {
        val tag = stored(context)
        if (tag.isBlank()) return context
        val locale = Locale.forLanguageTag(tag)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
