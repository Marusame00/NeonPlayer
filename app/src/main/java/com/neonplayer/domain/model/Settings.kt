package com.neonplayer.domain.model

import androidx.compose.runtime.Immutable

enum class AccentMode { CYAN, MAGENTA }

enum class VisualizerStyle { BARS, MIRRORED, RADIAL, OSCILLOSCOPE, RETRO_GRID }

enum class StageLayout { ART_LEFT, CENTERED_RING, TEXT_OVER_VISUALIZER, VISUALIZER_ONLY }

enum class OrientationLock { LANDSCAPE, AUTO, PORTRAIT }

enum class ReplayGainMode { OFF, TRACK, ALBUM }

const val BACKDROP_STATIC = "static"
const val BACKDROP_ASSET_PREFIX = "asset:"
const val BACKDROP_CUSTOM_PREFIX = "custom:"

@Immutable
data class AppSettings(
    val glassIcons: Boolean = true,
    val accent: String = AccentMode.CYAN.name,
    val backdropId: String = BACKDROP_STATIC,
    val backdropDim: Float = 0.45f,
    val backdropVignette: Boolean = true,
    val batterySaver: Boolean = false,
    val customBackdrops: List<String> = emptyList(),
    val visualizer: VisualizerStyle = VisualizerStyle.BARS,
    val visualizerSensitivity: Float = 1f,
    val visualizerArtColors: Boolean = false,
    val stageLayout: StageLayout = StageLayout.ART_LEFT,
    val stageShowAlbum: Boolean = true,
    val stageGlowSweep: Boolean = true,
    val autoStageSeconds: Int = 0,
    val keepScreenOnWhilePlaying: Boolean = false,
    val burnInProtection: Boolean = true,
    val dimAfterMinutes: Int = 0,
    val reduceMotion: Boolean = false,
    val orientation: OrientationLock = OrientationLock.LANDSCAPE,
    val crossfadeMs: Int = 0,
    val replayGain: ReplayGainMode = ReplayGainMode.OFF,
    val librarySort: SortOrder = SortOrder.TITLE,
    val libraryTab: LibraryTab = LibraryTab.SONGS,
    val volume: Float = 1f,
    val language: String = "",
)

@Immutable
data class EqProfile(
    val enabled: Boolean = false,
    val preset: String = "Flat",
    val bandLevelsMb: List<Int> = emptyList(),
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
)

data class SavedPlaybackState(
    val index: Int = 0,
    val positionMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
)
