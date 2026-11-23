package com.neonplayer.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.neonplayer.domain.model.AppSettings
import com.neonplayer.domain.model.EqProfile
import com.neonplayer.domain.model.RepeatMode
import com.neonplayer.domain.model.SavedPlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.json.JSONArray
import org.json.JSONObject

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(
    private val context: Context,
    private val scope: CoroutineScope,
) {

    val settings: StateFlow<AppSettings> = context.settingsDataStore.data
        .map { it.toSettings() }
        .stateIn(scope, SharingStarted.Eagerly, AppSettings())

    val eqProfiles: StateFlow<Map<String, EqProfile>> = context.settingsDataStore.data
        .map { decodeProfiles(it[Keys.EQ_PROFILES]) }
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[Keys.GLASS_ICONS] = next.glassIcons
            prefs[Keys.ACCENT] = next.accent.name
            prefs[Keys.BACKDROP_ID] = next.backdropId
            prefs[Keys.BACKDROP_DIM] = next.backdropDim
            prefs[Keys.BACKDROP_VIGNETTE] = next.backdropVignette
            prefs[Keys.BATTERY_SAVER] = next.batterySaver
            prefs[Keys.CUSTOM_BACKDROPS] = next.customBackdrops.joinToString("\n")
            prefs[Keys.VISUALIZER] = next.visualizer.name
            prefs[Keys.VISUALIZER_SENSITIVITY] = next.visualizerSensitivity
            prefs[Keys.VISUALIZER_ART_COLORS] = next.visualizerArtColors
            prefs[Keys.STAGE_LAYOUT] = next.stageLayout.name
            prefs[Keys.STAGE_SHOW_ALBUM] = next.stageShowAlbum
            prefs[Keys.STAGE_GLOW_SWEEP] = next.stageGlowSweep
            prefs[Keys.AUTO_STAGE_SECONDS] = next.autoStageSeconds
            prefs[Keys.KEEP_SCREEN_ON] = next.keepScreenOnWhilePlaying
            prefs[Keys.BURN_IN_PROTECTION] = next.burnInProtection
            prefs[Keys.DIM_AFTER_MINUTES] = next.dimAfterMinutes
            prefs[Keys.REDUCE_MOTION] = next.reduceMotion
            prefs[Keys.ORIENTATION] = next.orientation.name
            prefs[Keys.CROSSFADE_MS] = next.crossfadeMs
            prefs[Keys.REPLAY_GAIN] = next.replayGain.name
            prefs[Keys.LIBRARY_SORT] = next.librarySort.name
            prefs[Keys.LIBRARY_TAB] = next.libraryTab.name
            prefs[Keys.VOLUME] = next.volume
        }
    }

    suspend fun saveEqProfile(route: String, profile: EqProfile) {
        context.settingsDataStore.edit { prefs ->
            val profiles = decodeProfiles(prefs[Keys.EQ_PROFILES]).toMutableMap()
            profiles[route] = profile
            prefs[Keys.EQ_PROFILES] = encodeProfiles(profiles)
        }
    }

    suspend fun savePlaybackState(state: SavedPlaybackState) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.PLAYBACK_INDEX] = state.index
            prefs[Keys.PLAYBACK_POSITION] = state.positionMs
            prefs[Keys.PLAYBACK_SHUFFLE] = state.shuffle
            prefs[Keys.PLAYBACK_REPEAT] = state.repeat.name
        }
    }

    suspend fun loadPlaybackState(): SavedPlaybackState {
        val prefs = context.settingsDataStore.data.first()
        return SavedPlaybackState(
            index = prefs[Keys.PLAYBACK_INDEX] ?: 0,
            positionMs = prefs[Keys.PLAYBACK_POSITION] ?: 0L,
            shuffle = prefs[Keys.PLAYBACK_SHUFFLE] ?: false,
            repeat = enumOf(prefs[Keys.PLAYBACK_REPEAT], RepeatMode.OFF),
        )
    }

    private fun Preferences.toSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            glassIcons = this[Keys.GLASS_ICONS] ?: defaults.glassIcons,
            accent = enumOf(this[Keys.ACCENT], defaults.accent),
            backdropId = this[Keys.BACKDROP_ID] ?: defaults.backdropId,
            backdropDim = this[Keys.BACKDROP_DIM] ?: defaults.backdropDim,
            backdropVignette = this[Keys.BACKDROP_VIGNETTE] ?: defaults.backdropVignette,
            batterySaver = this[Keys.BATTERY_SAVER] ?: defaults.batterySaver,
            customBackdrops = this[Keys.CUSTOM_BACKDROPS]
                ?.split('\n')
                ?.filter { it.isNotBlank() }
                ?: defaults.customBackdrops,
            visualizer = enumOf(this[Keys.VISUALIZER], defaults.visualizer),
            visualizerSensitivity = this[Keys.VISUALIZER_SENSITIVITY] ?: defaults.visualizerSensitivity,
            visualizerArtColors = this[Keys.VISUALIZER_ART_COLORS] ?: defaults.visualizerArtColors,
            stageLayout = enumOf(this[Keys.STAGE_LAYOUT], defaults.stageLayout),
            stageShowAlbum = this[Keys.STAGE_SHOW_ALBUM] ?: defaults.stageShowAlbum,
            stageGlowSweep = this[Keys.STAGE_GLOW_SWEEP] ?: defaults.stageGlowSweep,
            autoStageSeconds = this[Keys.AUTO_STAGE_SECONDS] ?: defaults.autoStageSeconds,
            keepScreenOnWhilePlaying = this[Keys.KEEP_SCREEN_ON] ?: defaults.keepScreenOnWhilePlaying,
            burnInProtection = this[Keys.BURN_IN_PROTECTION] ?: defaults.burnInProtection,
            dimAfterMinutes = this[Keys.DIM_AFTER_MINUTES] ?: defaults.dimAfterMinutes,
            reduceMotion = this[Keys.REDUCE_MOTION] ?: defaults.reduceMotion,
            orientation = enumOf(this[Keys.ORIENTATION], defaults.orientation),
            crossfadeMs = this[Keys.CROSSFADE_MS] ?: defaults.crossfadeMs,
            replayGain = enumOf(this[Keys.REPLAY_GAIN], defaults.replayGain),
            librarySort = enumOf(this[Keys.LIBRARY_SORT], defaults.librarySort),
            libraryTab = enumOf(this[Keys.LIBRARY_TAB], defaults.libraryTab),
            volume = this[Keys.VOLUME] ?: defaults.volume,
        )
    }

    private inline fun <reified T : Enum<T>> enumOf(name: String?, fallback: T): T {
        if (name.isNullOrBlank()) return fallback
        return runCatching { enumValueOf<T>(name) }.getOrDefault(fallback)
    }

    private fun decodeProfiles(raw: String?): Map<String, EqProfile> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            val root = JSONObject(raw)
            val out = LinkedHashMap<String, EqProfile>()
            for (route in root.keys()) {
                val obj = root.optJSONObject(route) ?: continue
                val bands = obj.optJSONArray("bands")
                val levels = if (bands == null) emptyList() else (0 until bands.length()).map { bands.optInt(it) }
                out[route] = EqProfile(
                    enabled = obj.optBoolean("enabled", false),
                    preset = obj.optString("preset", "Flat"),
                    bandLevelsMb = levels,
                    bassBoost = obj.optInt("bassBoost", 0),
                    virtualizer = obj.optInt("virtualizer", 0),
                )
            }
            out
        }.getOrDefault(emptyMap())
    }

    private fun encodeProfiles(profiles: Map<String, EqProfile>): String {
        val root = JSONObject()
        for ((route, profile) in profiles) {
            val obj = JSONObject()
            obj.put("enabled", profile.enabled)
            obj.put("preset", profile.preset)
            val bands = JSONArray()
            profile.bandLevelsMb.forEach { bands.put(it) }
            obj.put("bands", bands)
            obj.put("bassBoost", profile.bassBoost)
            obj.put("virtualizer", profile.virtualizer)
            root.put(route, obj)
        }
        return root.toString()
    }

    private object Keys {
        val GLASS_ICONS = booleanPreferencesKey("glass_icons")
        val ACCENT = stringPreferencesKey("accent")
        val BACKDROP_ID = stringPreferencesKey("backdrop_id")
        val BACKDROP_DIM = floatPreferencesKey("backdrop_dim")
        val BACKDROP_VIGNETTE = booleanPreferencesKey("backdrop_vignette")
        val BATTERY_SAVER = booleanPreferencesKey("battery_saver")
        val CUSTOM_BACKDROPS = stringPreferencesKey("custom_backdrops")
        val VISUALIZER = stringPreferencesKey("visualizer")
        val VISUALIZER_SENSITIVITY = floatPreferencesKey("visualizer_sensitivity")
        val VISUALIZER_ART_COLORS = booleanPreferencesKey("visualizer_art_colors")
        val STAGE_LAYOUT = stringPreferencesKey("stage_layout")
        val STAGE_SHOW_ALBUM = booleanPreferencesKey("stage_show_album")
        val STAGE_GLOW_SWEEP = booleanPreferencesKey("stage_glow_sweep")
        val AUTO_STAGE_SECONDS = intPreferencesKey("auto_stage_seconds")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val BURN_IN_PROTECTION = booleanPreferencesKey("burn_in_protection")
        val DIM_AFTER_MINUTES = intPreferencesKey("dim_after_minutes")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val ORIENTATION = stringPreferencesKey("orientation")
        val CROSSFADE_MS = intPreferencesKey("crossfade_ms")
        val REPLAY_GAIN = stringPreferencesKey("replay_gain")
        val LIBRARY_SORT = stringPreferencesKey("library_sort")
        val LIBRARY_TAB = stringPreferencesKey("library_tab")
        val VOLUME = floatPreferencesKey("volume")
        val EQ_PROFILES = stringPreferencesKey("eq_profiles")
        val PLAYBACK_INDEX = intPreferencesKey("playback_index")
        val PLAYBACK_POSITION = longPreferencesKey("playback_position")
        val PLAYBACK_SHUFFLE = booleanPreferencesKey("playback_shuffle")
        val PLAYBACK_REPEAT = stringPreferencesKey("playback_repeat")
    }
}
