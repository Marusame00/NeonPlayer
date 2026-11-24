package com.neonplayer.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neonplayer.ui.backdrop.Backdrop
import com.neonplayer.ui.eq.EqualizerScreen
import com.neonplayer.ui.settings.BackdropPickerScreen
import com.neonplayer.ui.settings.SettingsScreen
import com.neonplayer.ui.stage.StageScreen
import kotlinx.coroutines.delay

@Composable
fun NeonPlayerApp(
    vm: MainViewModel,
    windowSizeClass: WindowSizeClass,
    audioPermissionGranted: Boolean,
    onGrantAccess: () -> Unit,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val stageMode by vm.stageMode.collectAsStateWithLifecycle()
    val overlay by vm.overlay.collectAsStateWithLifecycle()
    val player by vm.player.collectAsStateWithLifecycle()
    val upNext by vm.upNext.collectAsStateWithLifecycle()
    val lastInteraction by vm.lastInteractionMs.collectAsStateWithLifecycle()

    val reduceMotion = settings.reduceMotion
    val fadeMs = if (reduceMotion) 150 else 500

    var lastOverlay by remember { mutableStateOf(Overlay.SETTINGS) }
    val stateHolder = rememberSaveableStateHolder()
    LaunchedEffect(overlay) {
        if (overlay != Overlay.NONE) lastOverlay = overlay
    }

    BackHandler(enabled = overlay != Overlay.NONE || stageMode) {
        if (overlay != Overlay.NONE) vm.closeOverlay() else vm.exitStage()
    }

    LaunchedEffect(
        settings.autoStageSeconds,
        player.isPlaying,
        overlay,
        stageMode,
        lastInteraction,
    ) {
        val seconds = settings.autoStageSeconds
        if (seconds > 0 && player.isPlaying && overlay == Overlay.NONE && !stageMode) {
            delay(seconds * 1000L)
            vm.enterStage()
        }
    }

    Box(modifier = Modifier.fillMaxSize().imePadding()) {
        Backdrop(settings = settings)
        Crossfade(
            targetState = stageMode,
            animationSpec = tween(fadeMs),
            label = "stageMode",
        ) { stage ->
            if (stage) {
                StageScreen(
                    state = player,
                    positionMs = { vm.positionMs.value },
                    upNext = upNext,
                    settings = settings,
                    onExit = vm::exitStage,
                    onTogglePlay = vm::togglePlayPause,
                    onNext = vm::next,
                    onPrevious = vm::previous,
                    onSeek = vm::seekTo,
                )
            } else {
                stateHolder.SaveableStateProvider("main") {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                    ) {
                        val landscape = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact ||
                            maxWidth > maxHeight
                        if (landscape) {
                            LandscapeMain(
                                vm = vm,
                                audioPermissionGranted = audioPermissionGranted,
                                onGrantAccess = onGrantAccess,
                            )
                        } else {
                            PortraitMain(
                                vm = vm,
                                audioPermissionGranted = audioPermissionGranted,
                                onGrantAccess = onGrantAccess,
                            )
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = overlay != Overlay.NONE,
            enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 24 },
            exit = fadeOut(tween(180)) + slideOutVertically(tween(180)) { it / 24 },
        ) {
            when (if (overlay == Overlay.NONE) lastOverlay else overlay) {
                Overlay.SETTINGS -> SettingsScreen(vm = vm, onClose = vm::closeOverlay)
                Overlay.EQUALIZER -> EqualizerScreen(vm = vm, onClose = vm::closeOverlay)
                Overlay.BACKDROPS -> BackdropPickerScreen(vm = vm, onClose = vm::closeOverlay)
                Overlay.NONE -> Box(Modifier.fillMaxSize())
            }
        }
    }
}
