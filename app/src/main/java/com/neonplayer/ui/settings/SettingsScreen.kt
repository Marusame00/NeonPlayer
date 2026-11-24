package com.neonplayer.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neonplayer.BuildConfig
import com.neonplayer.domain.model.AccentMode
import com.neonplayer.domain.model.AppSettings
import com.neonplayer.domain.model.OrientationLock
import com.neonplayer.domain.model.ReplayGainMode
import com.neonplayer.domain.model.ScanState
import com.neonplayer.domain.model.StageLayout
import com.neonplayer.domain.model.VisualizerStyle
import com.neonplayer.ui.backdrop.BackdropCatalog
import com.neonplayer.ui.components.FlatIconButton
import com.neonplayer.ui.components.GlassButton
import com.neonplayer.ui.components.NeonCard
import com.neonplayer.ui.components.NeonChip
import com.neonplayer.ui.components.NeonIconButton
import com.neonplayer.ui.components.NeonSlider
import com.neonplayer.ui.components.NeonSwitch
import com.neonplayer.ui.components.NeonTextButton
import com.neonplayer.ui.components.SectionLabel
import com.neonplayer.ui.components.SettingRow
import com.neonplayer.ui.components.gridTexture
import com.neonplayer.ui.library.ScanStatusText
import com.neonplayer.ui.main.MainViewModel
import com.neonplayer.ui.main.Overlay
import com.neonplayer.ui.theme.Neon
import com.neonplayer.ui.theme.NeonType

@Composable
fun SettingsScreen(
    vm: MainViewModel,
    onClose: () -> Unit,
) {
    val palette = Neon.palette
    val settings by vm.settings.collectAsStateWithLifecycle()
    val scanState by vm.scanState.collectAsStateWithLifecycle()
    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> if (uri != null) vm.importFolder(uri) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.base0.copy(alpha = 0.92f))
            .gridTexture(palette.primary.copy(alpha = 0.035f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NeonIconButton(
                        icon = Icons.Rounded.ArrowBack,
                        contentDescription = "Close settings",
                        onClick = onClose,
                        size = 34.dp,
                    )
                    SectionLabel(text = "Settings", color = palette.primary)
                }
                Spacer(Modifier.height(8.dp))
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val landscape = maxWidth > maxHeight
                    if (landscape) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                AppearanceSection(vm, settings)
                                BackdropSection(vm, settings)
                                VisualizerSection(vm, settings)
                                DisplaySection(vm, settings)
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                StageSection(vm, settings)
                                PlaybackSection(vm, settings)
                                LibrarySection(
                                    vm = vm,
                                    scanState = scanState,
                                    onAddFolder = { folderLauncher.launch(null) },
                                )
                                AboutSection()
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            AppearanceSection(vm, settings)
                            BackdropSection(vm, settings)
                            VisualizerSection(vm, settings)
                            StageSection(vm, settings)
                            PlaybackSection(vm, settings)
                            DisplaySection(vm, settings)
                            LibrarySection(
                                vm = vm,
                                scanState = scanState,
                                onAddFolder = { folderLauncher.launch(null) },
                            )
                            AboutSection()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    NeonCard(modifier = Modifier.fillMaxWidth(), glow = false) {
        SectionLabel(text = title)
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun AppearanceSection(vm: MainViewModel, settings: AppSettings) {
    SettingsSection(title = "Appearance") {
        SettingRow(
            title = "Glass icons",
            trailing = {
                NeonSwitch(
                    checked = settings.glassIcons,
                    onCheckedChange = { value ->
                        vm.updateSettings { it.copy(glassIcons = value) }
                    },
                )
            },
        )
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassButton(
                icon = Icons.Rounded.GraphicEq,
                contentDescription = "Glass preview",
                onClick = {},
                size = 40.dp,
            )
            FlatIconButton(
                icon = Icons.Rounded.GraphicEq,
                contentDescription = "Flat preview",
                onClick = {},
                size = 40.dp,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(text = "Theme accent", style = NeonType.BodySmall, color = Neon.palette.textSecondary)
        Spacer(Modifier.height(4.dp))
        ChipRow {
            AccentMode.entries.forEach { mode ->
                NeonChip(
                    text = mode.label,
                    selected = settings.accent == mode,
                    onClick = { vm.updateSettings { it.copy(accent = mode) } },
                )
            }
        }
        SettingRow(
            title = "Reduce motion",
            subtitle = "Disables pulses and animations",
            trailing = {
                NeonSwitch(
                    checked = settings.reduceMotion,
                    onCheckedChange = { value ->
                        vm.updateSettings { it.copy(reduceMotion = value) }
                    },
                )
            },
        )
    }
}

@Composable
private fun BackdropSection(vm: MainViewModel, settings: AppSettings) {
    val context = LocalContext.current
    val current = BackdropCatalog.resolve(context, settings.backdropId, settings.customBackdrops)
    SettingsSection(title = "Backdrop") {
        SettingRow(
            title = current.label,
            subtitle = "Current backdrop",
            trailing = {
                NeonTextButton(
                    text = "Choose",
                    onClick = { vm.openOverlay(Overlay.BACKDROPS) },
                )
            },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Dim ${(settings.backdropDim * 100f).toInt()}%",
            style = NeonType.BodySmall,
            color = Neon.palette.textSecondary,
        )
        NeonSlider(
            value = settings.backdropDim,
            onValueChange = { value -> vm.updateSettings { it.copy(backdropDim = value) } },
            valueRange = 0f..0.8f,
        )
        SettingRow(
            title = "Vignette",
            trailing = {
                NeonSwitch(
                    checked = settings.backdropVignette,
                    onCheckedChange = { value ->
                        vm.updateSettings { it.copy(backdropVignette = value) }
                    },
                )
            },
        )
        SettingRow(
            title = "Battery saver",
            subtitle = "Use the static image instead of video",
            trailing = {
                NeonSwitch(
                    checked = settings.batterySaver,
                    onCheckedChange = { value ->
                        vm.updateSettings { it.copy(batterySaver = value) }
                    },
                )
            },
        )
    }
}

@Composable
private fun VisualizerSection(vm: MainViewModel, settings: AppSettings) {
    SettingsSection(title = "Visualizer") {
        ChipRow {
            VisualizerStyle.entries.forEach { style ->
                NeonChip(
                    text = style.label,
                    selected = settings.visualizer == style,
                    onClick = { vm.updateSettings { it.copy(visualizer = style) } },
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Sensitivity ${"%.1f".format(settings.visualizerSensitivity)}×",
            style = NeonType.BodySmall,
            color = Neon.palette.textSecondary,
        )
        NeonSlider(
            value = settings.visualizerSensitivity,
            onValueChange = { value -> vm.updateSettings { it.copy(visualizerSensitivity = value) } },
            valueRange = 0.5f..2f,
        )
        SettingRow(
            title = "Colors from album art",
            trailing = {
                NeonSwitch(
                    checked = settings.visualizerArtColors,
                    onCheckedChange = { value ->
                        vm.updateSettings { it.copy(visualizerArtColors = value) }
                    },
                )
            },
        )
    }
}

@Composable
private fun StageSection(vm: MainViewModel, settings: AppSettings) {
    SettingsSection(title = "Stage mode") {
        ChipRow {
            StageLayout.entries.forEach { layout ->
                NeonChip(
                    text = layout.label,
                    selected = settings.stageLayout == layout,
                    onClick = { vm.updateSettings { it.copy(stageLayout = layout) } },
                )
            }
        }
        SettingRow(
            title = "Show album",
            trailing = {
                NeonSwitch(
                    checked = settings.stageShowAlbum,
                    onCheckedChange = { value ->
                        vm.updateSettings { it.copy(stageShowAlbum = value) }
                    },
                )
            },
        )
        SettingRow(
            title = "Track-change glow sweep",
            trailing = {
                NeonSwitch(
                    checked = settings.stageGlowSweep,
                    onCheckedChange = { value ->
                        vm.updateSettings { it.copy(stageGlowSweep = value) }
                    },
                )
            },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Auto-enter Stage",
            style = NeonType.BodySmall,
            color = Neon.palette.textSecondary,
        )
        Spacer(Modifier.height(4.dp))
        ChipRow {
            AUTO_STAGE_OPTIONS.forEach { seconds ->
                NeonChip(
                    text = autoStageLabel(seconds),
                    selected = settings.autoStageSeconds == seconds,
                    onClick = { vm.updateSettings { it.copy(autoStageSeconds = seconds) } },
                )
            }
        }
        SettingRow(
            title = "Keep screen on while playing",
            subtitle = "Stage Mode always keeps the screen awake",
            trailing = {
                NeonSwitch(
                    checked = settings.keepScreenOnWhilePlaying,
                    onCheckedChange = { value ->
                        vm.updateSettings { it.copy(keepScreenOnWhilePlaying = value) }
                    },
                )
            },
        )
        SettingRow(
            title = "Burn-in protection",
            subtitle = "Slow pixel drift of art and text",
            trailing = {
                NeonSwitch(
                    checked = settings.burnInProtection,
                    onCheckedChange = { value ->
                        vm.updateSettings { it.copy(burnInProtection = value) }
                    },
                )
            },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Dim after",
            style = NeonType.BodySmall,
            color = Neon.palette.textSecondary,
        )
        Spacer(Modifier.height(4.dp))
        ChipRow {
            DIM_AFTER_OPTIONS.forEach { minutes ->
                NeonChip(
                    text = if (minutes == 0) "Off" else "${minutes}m",
                    selected = settings.dimAfterMinutes == minutes,
                    onClick = { vm.updateSettings { it.copy(dimAfterMinutes = minutes) } },
                )
            }
        }
    }
}

@Composable
private fun PlaybackSection(vm: MainViewModel, settings: AppSettings) {
    SettingsSection(title = "Playback") {
        Text(
            text = "Crossfade",
            style = NeonType.BodySmall,
            color = Neon.palette.textSecondary,
        )
        Spacer(Modifier.height(4.dp))
        ChipRow {
            CROSSFADE_OPTIONS.forEach { ms ->
                NeonChip(
                    text = if (ms == 0) "Off" else "${ms / 1000}s",
                    selected = settings.crossfadeMs == ms,
                    onClick = { vm.updateSettings { it.copy(crossfadeMs = ms) } },
                )
            }
        }
        Text(
            text = "Fades out/in between tracks; gapless when off",
            style = NeonType.BodySmall,
            color = Neon.palette.textMuted,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "ReplayGain",
            style = NeonType.BodySmall,
            color = Neon.palette.textSecondary,
        )
        Spacer(Modifier.height(4.dp))
        ChipRow {
            ReplayGainMode.entries.forEach { mode ->
                NeonChip(
                    text = mode.label,
                    selected = settings.replayGain == mode,
                    onClick = { vm.updateSettings { it.copy(replayGain = mode) } },
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Sleep timer",
            style = NeonType.BodySmall,
            color = Neon.palette.textSecondary,
        )
        Spacer(Modifier.height(4.dp))
        ChipRow {
            NeonChip(text = "Off", selected = false, onClick = { vm.cancelSleepTimer() })
            SLEEP_OPTIONS.forEach { minutes ->
                NeonChip(
                    text = "$minutes min",
                    selected = false,
                    onClick = { vm.startSleepTimer(minutes) },
                )
            }
            NeonChip(
                text = "End of track",
                selected = false,
                onClick = { vm.startSleepTimerEndOfTrack() },
            )
        }
        Spacer(Modifier.height(6.dp))
        NeonTextButton(
            text = "Equalizer",
            icon = Icons.Rounded.GraphicEq,
            onClick = { vm.openOverlay(Overlay.EQUALIZER) },
        )
    }
}

@Composable
private fun DisplaySection(vm: MainViewModel, settings: AppSettings) {
    SettingsSection(title = "Display") {
        ChipRow {
            OrientationLock.entries.forEach { lock ->
                NeonChip(
                    text = lock.label,
                    selected = settings.orientation == lock,
                    onClick = { vm.updateSettings { it.copy(orientation = lock) } },
                )
            }
        }
    }
}

@Composable
private fun LibrarySection(
    vm: MainViewModel,
    scanState: ScanState,
    onAddFolder: () -> Unit,
) {
    SettingsSection(title = "Library") {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeonTextButton(
                text = "Rescan now",
                icon = Icons.Rounded.Refresh,
                primary = true,
                onClick = vm::rescan,
            )
            NeonTextButton(
                text = "Add folder",
                icon = Icons.Rounded.CreateNewFolder,
                onClick = onAddFolder,
            )
        }
        Spacer(Modifier.height(6.dp))
        ScanStatusText(scanState = scanState)
    }
}

@Composable
private fun AboutSection() {
    val palette = Neon.palette
    SettingsSection(title = "About") {
        Text(
            text = "NeonPlayer ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
            style = NeonType.Body,
            color = palette.textPrimary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Backdrop videos: app/src/main/assets/backdrops",
            style = NeonType.BodySmall,
            color = palette.textMuted,
        )
    }
}

private fun autoStageLabel(seconds: Int): String = when {
    seconds <= 0 -> "Off"
    seconds < 60 -> "${seconds}s"
    else -> "${seconds / 60}m"
}

private val AUTO_STAGE_OPTIONS = listOf(0, 15, 30, 60, 120, 300)
private val DIM_AFTER_OPTIONS = listOf(0, 1, 5, 10, 30)
private val CROSSFADE_OPTIONS = listOf(0, 2000, 4000, 6000, 8000)
private val SLEEP_OPTIONS = listOf(15, 30, 45, 60, 90)
