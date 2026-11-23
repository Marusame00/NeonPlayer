package com.neonplayer.playback

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Handler
import android.os.Looper
import com.neonplayer.data.settings.SettingsRepository
import com.neonplayer.domain.model.EqProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.ln

data class EqUiState(
    val available: Boolean = false,
    val enabled: Boolean = false,
    val bandFreqsHz: List<Int> = emptyList(),
    val minMb: Int = -1500,
    val maxMb: Int = 1500,
    val levelsMb: List<Int> = emptyList(),
    val presets: List<String> = emptyList(),
    val preset: String = "Flat",
    val bassBoostSupported: Boolean = false,
    val bassBoost: Int = 0,
    val virtualizerSupported: Boolean = false,
    val virtualizer: Int = 0,
    val route: String = "speaker",
)

class EqualizerManager(
    private val context: Context,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope,
) {

    private val _state = MutableStateFlow(EqUiState())
    val state: StateFlow<EqUiState> = _state.asStateFlow()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var sessionId = 0
    private var saveJob: Job? = null
    private var deviceCallback: AudioDeviceCallback? = null
    private var route = "speaker"

    init {
        route = detectRoute()
        _state.value = _state.value.copy(route = route, presets = PRESET_NAMES)
        registerDeviceCallback()
    }

    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0) return
        if (sessionId == audioSessionId && equalizer != null) return
        release()
        sessionId = audioSessionId
        val eq = runCatching { Equalizer(0, audioSessionId) }.getOrNull()
        equalizer = eq
        bassBoost = runCatching { BassBoost(0, audioSessionId) }.getOrNull()
        virtualizer = runCatching { Virtualizer(0, audioSessionId) }.getOrNull()
        if (eq == null) {
            _state.value = EqUiState(route = route, presets = PRESET_NAMES)
            return
        }
        val freqs = runCatching {
            (0 until eq.numberOfBands.toInt()).map { eq.getCenterFreq(it.toShort()) / 1000 }
        }.getOrDefault(emptyList())
        val range = runCatching { eq.bandLevelRange }.getOrNull()
        val minMb = range?.getOrNull(0)?.toInt() ?: -1500
        val maxMb = range?.getOrNull(1)?.toInt() ?: 1500
        _state.value = EqUiState(
            available = true,
            enabled = runCatching { eq.enabled }.getOrDefault(false),
            bandFreqsHz = freqs,
            minMb = minMb,
            maxMb = maxMb,
            levelsMb = freqs.map { 0 },
            presets = PRESET_NAMES,
            preset = "Flat",
            bassBoostSupported = bassBoost != null,
            virtualizerSupported = virtualizer != null,
            route = route,
        )
        applyProfile(settings.eqProfiles.value[route])
    }

    fun release() {
        saveJob?.cancel()
        saveJob = null
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        sessionId = 0
        _state.value = EqUiState(route = route, presets = PRESET_NAMES)
    }

    fun setEnabled(enabled: Boolean) {
        val eq = equalizer ?: return
        runCatching { eq.enabled = enabled }
        _state.value = _state.value.copy(enabled = enabled)
        scheduleSave()
    }

    fun setBandLevel(index: Int, mb: Int) {
        val eq = equalizer ?: return
        val current = _state.value
        if (index !in current.levelsMb.indices) return
        val clamped = mb.coerceIn(current.minMb, current.maxMb)
        runCatching { eq.setBandLevel(index.toShort(), clamped.toShort()) }
        val levels = current.levelsMb.toMutableList()
        levels[index] = clamped
        _state.value = current.copy(levelsMb = levels, preset = "Custom")
        scheduleSave()
    }

    fun applyPreset(name: String) {
        val eq = equalizer ?: return
        if (name == CUSTOM_PRESET) {
            _state.value = _state.value.copy(preset = CUSTOM_PRESET)
            scheduleSave()
            return
        }
        val gains = PRESETS[name] ?: return
        val current = _state.value
        val levels = current.bandFreqsHz.map { freq ->
            (interpolate(gains, freq.toFloat()) * 100f).toInt().coerceIn(current.minMb, current.maxMb)
        }
        levels.forEachIndexed { index, mb ->
            runCatching { eq.setBandLevel(index.toShort(), mb.toShort()) }
        }
        _state.value = current.copy(preset = name, levelsMb = levels)
        scheduleSave()
    }

    fun setBassBoost(strength: Int) {
        val effect = bassBoost ?: return
        val clamped = strength.coerceIn(0, 1000)
        runCatching { effect.setStrength(clamped.toShort()) }
        runCatching { effect.enabled = clamped > 0 }
        _state.value = _state.value.copy(bassBoost = clamped)
        scheduleSave()
    }

    fun setVirtualizer(strength: Int) {
        val effect = virtualizer ?: return
        val clamped = strength.coerceIn(0, 1000)
        runCatching { effect.setStrength(clamped.toShort()) }
        runCatching { effect.enabled = clamped > 0 }
        _state.value = _state.value.copy(virtualizer = clamped)
        scheduleSave()
    }

    private fun applyProfile(profile: EqProfile?) {
        val eq = equalizer ?: return
        if (profile == null) {
            runCatching { eq.enabled = false }
            _state.value = _state.value.copy(enabled = false, preset = "Flat")
            return
        }
        runCatching { eq.enabled = profile.enabled }
        if (profile.preset != CUSTOM_PRESET && PRESETS.containsKey(profile.preset)) {
            val gains = PRESETS.getValue(profile.preset)
            val current = _state.value
            val levels = current.bandFreqsHz.map { freq ->
                (interpolate(gains, freq.toFloat()) * 100f).toInt().coerceIn(current.minMb, current.maxMb)
            }
            levels.forEachIndexed { index, mb ->
                runCatching { eq.setBandLevel(index.toShort(), mb.toShort()) }
            }
            _state.value = current.copy(preset = profile.preset, levelsMb = levels)
        } else if (profile.bandLevelsMb.isNotEmpty()) {
            profile.bandLevelsMb.forEachIndexed { index, mb ->
                runCatching { eq.setBandLevel(index.toShort(), mb.toShort()) }
            }
            _state.value = _state.value.copy(preset = profile.preset, levelsMb = profile.bandLevelsMb)
        }
        runCatching { bassBoost?.setStrength(profile.bassBoost.toShort()) }
        runCatching { bassBoost?.enabled = profile.bassBoost > 0 }
        runCatching { virtualizer?.setStrength(profile.virtualizer.toShort()) }
        runCatching { virtualizer?.enabled = profile.virtualizer > 0 }
        _state.value = _state.value.copy(
            enabled = profile.enabled,
            bassBoost = profile.bassBoost,
            virtualizer = profile.virtualizer,
        )
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(SAVE_DEBOUNCE_MS)
            val current = _state.value
            settings.saveEqProfile(
                route,
                EqProfile(
                    enabled = current.enabled,
                    preset = current.preset,
                    bandLevelsMb = current.levelsMb,
                    bassBoost = current.bassBoost,
                    virtualizer = current.virtualizer,
                ),
            )
        }
    }

    private fun registerDeviceCallback() {
        val manager = audioManager ?: return
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                onRouteChanged()
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                onRouteChanged()
            }
        }
        deviceCallback = callback
        runCatching { manager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper())) }
    }

    private fun onRouteChanged() {
        val detected = detectRoute()
        if (detected == route) return
        route = detected
        _state.value = _state.value.copy(route = route)
        applyProfile(settings.eqProfiles.value[route])
    }

    private fun detectRoute(): String {
        val manager = audioManager ?: return "speaker"
        val devices = runCatching { manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) }.getOrNull()
            ?: return "speaker"
        var wired = false
        for (device in devices) {
            when (device.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLE_SPEAKER,
                AudioDeviceInfo.TYPE_BLE_BROADCAST,
                -> return "bluetooth"

                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_USB_DEVICE,
                -> wired = true
            }
        }
        return if (wired) "wired" else "speaker"
    }

    private fun interpolate(gains: FloatArray, freq: Float): Float {
        if (freq <= REFERENCE_FREQS[0]) return gains[0]
        if (freq >= REFERENCE_FREQS[REFERENCE_FREQS.lastIndex]) return gains[gains.lastIndex]
        for (i in 0 until REFERENCE_FREQS.lastIndex) {
            val low = REFERENCE_FREQS[i]
            val high = REFERENCE_FREQS[i + 1]
            if (freq >= low && freq <= high) {
                val t = (ln(freq) - ln(low)) / (ln(high) - ln(low))
                return gains[i] + (gains[i + 1] - gains[i]) * t
            }
        }
        return 0f
    }

    private companion object {
        const val CUSTOM_PRESET = "Custom"
        const val SAVE_DEBOUNCE_MS = 300L
        val REFERENCE_FREQS = floatArrayOf(60f, 230f, 910f, 3600f, 14000f)
        val PRESETS: Map<String, FloatArray> = linkedMapOf(
            "Flat" to floatArrayOf(0f, 0f, 0f, 0f, 0f),
            "Bass Boost" to floatArrayOf(6f, 4f, 0f, 0f, 0f),
            "Vocal" to floatArrayOf(-2f, 0f, 3f, 4f, 1f),
            "Synthwave" to floatArrayOf(5f, 2f, -1f, 3f, 5f),
            "Rock" to floatArrayOf(4f, 2f, -1f, 2f, 4f),
            "Electronic" to floatArrayOf(5f, 3f, 0f, 2f, 4f),
            "Acoustic" to floatArrayOf(3f, 1f, 1f, 2f, 2f),
            "Treble Boost" to floatArrayOf(0f, 0f, 0f, 3f, 6f),
            "Night" to floatArrayOf(-2f, 0f, 1f, 1f, -3f),
        )
        val PRESET_NAMES: List<String> = PRESETS.keys.toList() + CUSTOM_PRESET
    }
}
