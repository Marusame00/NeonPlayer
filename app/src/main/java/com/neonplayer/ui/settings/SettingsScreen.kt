package com.neonplayer.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neonplayer.BuildConfig
import com.neonplayer.R
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
import com.neonplayer.ui.components.label
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
                        contentDescription = stringResource(R.string.settings_close),
                        onClick = onClose,
                        size = 34.dp,
                    )
                    SectionLabel(text = stringResource(R.string.label_settings), color = palette.primary)
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
                                LanguageSection(vm, settings)
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
                            LanguageSection(vm, settings)
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
    val context = LocalContext.current
    SettingsSection(title = stringResource(R.string.label_appearance)) {
        SettingRow(
            title = stringResource(R.string.settings_glass_icons),
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
                contentDescription = stringResource(R.string.settings_glass_preview),
                onClick = { vm.updateSettings { it.copy(glassIcons = true) } },
                size = 40.dp,
                active = settings.glassIcons,
            )
            FlatIconButton(
                icon = Icons.Rounded.GraphicEq,
                contentDescription = stringResource(R.string.settings_flat_preview),
                onClick = { vm.updateSettings { it.copy(glassIcons = false) } },
                size = 40.dp,
                active = !settings.glassIcons,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.settings_theme_accent),
            style = NeonType.BodySmall,
            color = Neon.palette.textSecondary,
        )
        Spacer(Modifier.height(4.dp))
        ChipRow {
            AccentMode.entries.forEach { mode ->
                NeonChip(
                    text = mode.label(context),
                    selected = settings.accent == mode,
                    onClick = { vm.updateSettings { it.copy(accent = mode) } },
                )
            }
        }
        SettingRow(
            title = stringResource(R.string.settings_reduce_motion),
            subtitle = stringResource(R.string.settings_reduce_motion_sub),
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
    SettingsSection(title = stringResource(R.string.label_backdrops)) {
        SettingRow(
            title = current.label,
            subtitle = stringResource(R.string.settings_backdrop_current),
            trailing = {
                NeonTextButton(
                    text = stringResource(R.string.action_choose),
                    onClick = { vm.openOverlay(Overlay.BACKDROPS) },
                )
            },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.settings_dim_percent,
                (settings.backdropDim * 100f).toInt(),
            ),
            style = NeonType.BodySmall,
            color = Neon.palette.textSecondary,
        )
        NeonSlider(
            value = settings.backdropDim,
            onValueChange = { value -> vm.updateSettings { it.copy(backdropDim = value) } },
            valueRange = 0f..0.8f,
        )
        SettingRow(
            title = stringResource(R.string.settings_vignette),
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
            title = stringResource(R.string.settings_battery_saver),
            subtitle = stringResource(R.string.settings_battery_saver_sub),
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
    val context = LocalContext.current
    SettingsSection(title = stringResource(R.string.label_visualizer)) {
        ChipRow {
            VisualizerStyle.entries.forEach { style ->
                NeonChip(
                    text = style.label(context),
                    selected = settings.visualizer == style,
                    onClick = { vm.updateSettings { it.copy(visualizer = style) } },
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(
                R.string.settings_sensitivity,
                "%.1f".format(settings.visualizerSensitivity),
            ),
            style = NeonType.BodySmall,
            color = Neon.palette.textSecondary,
        )
        NeonSlider(
            value = settings.visualizerSensitivity,
            onValueChange = { value -> vm.updateSettings { it.copy(visualizerSensitivity = value) } },
            valueRange = 0.5f..2f,
        )
        SettingRow(
            title = stringResource(R.string.settings_art_colors),
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
    val context = LocalContext.current
    SettingsSection(title = stringResource(R.string.label_stage_mode)) {
        ChipRow {
            StageLayout.entries.forEach { layout ->
                NeonChip(
                    text = layout.label(context),
                    selected = settings.stageLayout == layout,
                    onClick = { vm.updateSettings { it.copy(stageLayout = layout) } },
                )
            }
        }
        SettingRow(
            title = stringResource(R.string.settings_show_album),
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
            title = stringResource(R.string.settings_glow_sweep),
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
            text = stringResource(R.string.settings_auto_stage),
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
            title = stringResource(R.string.settings_keep_screen_on),
            subtitle = stringResource(R.string.settings_keep_screen_on_sub),
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
            title = stringResource(R.string.settings_burn_in),
            subtitle = stringResource(R.string.settings_burn_in_sub),
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
            text = stringResource(R.string.settings_dim_after),
            style = NeonType.BodySmall,
            color = Neon.palette.textSecondary,
        )
        Spacer(Modifier.height(4.dp))
        ChipRow {
            DIM_AFTER_OPTIONS.forEach { minutes ->
                NeonChip(
                    text = if (minutes == 0) {
                        stringResource(R.string.action_off)
                    } else {
                        stringResource(R.string.settings_minutes_short, minutes)
                    },
                    selected = settings.dimAfterMinutes == minutes,
                    onClick = { vm.updateSettings { it.copy(dimAfterMinutes = minutes) } },
                )
            }
        }
    }
}

@Composable
private fun PlaybackSection(vm: MainViewModel, settings: AppSettings) {
    val context = LocalContext.current
    SettingsSection(title = stringResource(R.string.label_playback)) {
        Text(
            text = stringResource(R.string.settings_crossfade),
            style = NeonType.BodySmall,
            color = Neon.palette.textSecondary,
        )
        Spacer(Modifier.height(4.dp))
        ChipRow {
            CROSSFADE_OPTIONS.forEach { ms ->
                NeonChip(
                    text = if (ms == 0) {
                        stringResource(R.string.action_off)
                    } else {
                        stringResource(R.string.settings_seconds_short, ms / 1000)
                    },
                    selected = settings.crossfadeMs == ms,
                    onClick = { vm.updateSettings { it.copy(crossfadeMs = ms) } },
                )
            }
        }
        Text(
            text = stringResource(R.string.settings_crossfade_sub),
            style = NeonType.BodySmall,
            color = Neon.palette.textMuted,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.settings_replay_gain),
            style = NeonType.BodySmall,
            color = Neon.palette.textSecondary,
        )
        Spacer(Modifier.height(4.dp))
        ChipRow {
            ReplayGainMode.entries.forEach { mode ->
                NeonChip(
                    text = mode.label(context),
                    selected = settings.replayGain == mode,
                    onClick = { vm.updateSettings { it.copy(replayGain = mode) } },
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.settings_sleep_timer),
            style = NeonType.BodySmall,
            color = Neon.palette.textSecondary,
        )
        Spacer(Modifier.height(4.dp))
        ChipRow {
            NeonChip(
                text = stringResource(R.string.action_off),
                selected = false,
                onClick = { vm.cancelSleepTimer() },
            )
            SLEEP_OPTIONS.forEach { minutes ->
                NeonChip(
                    text = stringResource(R.string.settings_sleep_min, minutes),
                    selected = false,
                    onClick = { vm.startSleepTimer(minutes) },
                )
            }
            NeonChip(
                text = stringResource(R.string.settings_sleep_end_of_track),
                selected = false,
                onClick = { vm.startSleepTimerEndOfTrack() },
            )
        }
        Spacer(Modifier.height(6.dp))
        NeonTextButton(
            text = stringResource(R.string.label_equalizer),
            icon = Icons.Rounded.GraphicEq,
            onClick = { vm.openOverlay(Overlay.EQUALIZER) },
        )
    }
}

@Composable
private fun DisplaySection(vm: MainViewModel, settings: AppSettings) {
    val context = LocalContext.current
    SettingsSection(title = stringResource(R.string.label_display)) {
        ChipRow {
            OrientationLock.entries.forEach { lock ->
                NeonChip(
                    text = lock.label(context),
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
    SettingsSection(title = stringResource(R.string.label_library)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeonTextButton(
                text = stringResource(R.string.settings_rescan_now),
                icon = Icons.Rounded.Refresh,
                primary = true,
                onClick = vm::rescan,
            )
            NeonTextButton(
                text = stringResource(R.string.settings_add_folder),
                icon = Icons.Rounded.CreateNewFolder,
                onClick = onAddFolder,
            )
        }
        Spacer(Modifier.height(6.dp))
        ScanStatusText(scanState = scanState)
    }
}

@Composable
private fun LanguageSection(vm: MainViewModel, settings: AppSettings) {
    val context = LocalContext.current
    SettingsSection(title = stringResource(R.string.label_language)) {
        SettingRow(
            title = stringResource(R.string.settings_language_sub),
            subtitle = stringResource(R.string.settings_language_note),
        )
        Spacer(Modifier.height(4.dp))
        ChipRow {
            LANGUAGE_OPTIONS.forEach { option ->
                NeonChip(
                    text = context.getString(option.labelRes),
                    selected = settings.language == option.tag,
                    onClick = { vm.updateSettings { it.copy(language = option.tag) } },
                )
            }
        }
    }
}

@Composable
private fun AboutSection() {
    val palette = Neon.palette
    SettingsSection(title = stringResource(R.string.label_about)) {
        Text(
            text = stringResource(
                R.string.settings_version,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
            ),
            style = NeonType.Body,
            color = palette.textPrimary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.settings_backdrop_path),
            style = NeonType.BodySmall,
            color = palette.textMuted,
        )
    }
}

@Composable
private fun autoStageLabel(seconds: Int): String = when {
    seconds <= 0 -> stringResource(R.string.action_off)
    seconds < 60 -> stringResource(R.string.settings_seconds_short, seconds)
    else -> stringResource(R.string.settings_minutes_short, seconds / 60)
}

private data class LanguageOption(val tag: String, @StringRes val labelRes: Int)

private val LANGUAGE_OPTIONS = listOf(
    LanguageOption("", R.string.lang_system),
    LanguageOption("en", R.string.lang_en),
    LanguageOption("es", R.string.lang_es),
    LanguageOption("pt-BR", R.string.lang_pt_br),
    LanguageOption("fr", R.string.lang_fr),
    LanguageOption("de", R.string.lang_de),
    LanguageOption("it", R.string.lang_it),
    LanguageOption("ru", R.string.lang_ru),
    LanguageOption("pl", R.string.lang_pl),
    LanguageOption("tr", R.string.lang_tr),
    LanguageOption("hi", R.string.lang_hi),
    LanguageOption("zh-TW", R.string.lang_zh_tw),
    LanguageOption("zh-CN", R.string.lang_zh_cn),
    LanguageOption("ja", R.string.lang_ja),
    LanguageOption("ko", R.string.lang_ko),
    LanguageOption("id", R.string.lang_id),
)

private val AUTO_STAGE_OPTIONS = listOf(0, 15, 30, 60, 120, 300)
private val SLEEP_OPTIONS = listOf(15, 30, 45, 60, 90)
private val DIM_AFTER_OPTIONS = listOf(0, 1, 5, 10, 30)
private val CROSSFADE_OPTIONS = listOf(0, 2000, 4000, 6000, 8000)
