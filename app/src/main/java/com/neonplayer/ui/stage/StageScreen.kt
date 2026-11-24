package com.neonplayer.ui.stage

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.neonplayer.R
import com.neonplayer.domain.model.AppSettings
import com.neonplayer.domain.model.StageLayout
import com.neonplayer.domain.model.Track
import com.neonplayer.domain.model.VisualizerStyle
import com.neonplayer.domain.model.formatDuration
import com.neonplayer.playback.PlayerUiState
import com.neonplayer.playback.QueueEntry
import com.neonplayer.ui.components.ArtImage
import com.neonplayer.ui.components.NeonCard
import com.neonplayer.ui.components.NeonIconButton
import com.neonplayer.ui.components.PlayPauseButton
import com.neonplayer.ui.components.SeekBar
import com.neonplayer.ui.components.SectionLabel
import com.neonplayer.ui.theme.Neon
import com.neonplayer.ui.theme.NeonType
import com.neonplayer.ui.visualizer.Visualizer
import com.neonplayer.ui.visualizer.rememberArtAccents
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs

@Composable
fun StageScreen(
    state: PlayerUiState,
    positionMs: () -> Long,
    upNext: List<QueueEntry>,
    settings: AppSettings,
    onExit: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Neon.palette
    val reduceMotion = settings.reduceMotion || Neon.style.reduceMotion
    val view = LocalView.current
    val density = LocalDensity.current
    val currentTogglePlay by rememberUpdatedState(onTogglePlay)
    val currentNext by rememberUpdatedState(onNext)
    val currentPrevious by rememberUpdatedState(onPrevious)

    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var controlsVisible by remember { mutableStateOf(true) }
    var queueVisible by remember { mutableStateOf(false) }

    val accents = rememberArtAccents(state.currentTrack, settings.visualizerArtColors)
    val primary = accents?.first ?: palette.primary
    val secondary = accents?.second ?: palette.secondary

    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(lastInteraction) {
        controlsVisible = true
        delay(4000)
        controlsVisible = false
    }

    LaunchedEffect(queueVisible, lastInteraction) {
        if (!queueVisible) return@LaunchedEffect
        delay(6000)
        queueVisible = false
    }

    val sweep = remember { Animatable(0f) }
    LaunchedEffect(state.currentTrack?.id) {
        if (settings.stageGlowSweep && !reduceMotion && state.currentTrack != null) {
            sweep.snapTo(0f)
            sweep.animateTo(1f, tween(durationMillis = 1200, easing = LinearEasing))
        }
    }

    val swipeThreshold = with(density) { 80.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var lastTap = 0L
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var dx = 0f
                    var dy = 0f
                    var dragging = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        dx += change.positionChange().x
                        dy += change.positionChange().y
                        if (!dragging && (abs(dx) > viewConfiguration.touchSlop || abs(dy) > viewConfiguration.touchSlop)) {
                            dragging = true
                        }
                        if (dragging) change.consume()
                    }
                    if (dragging) {
                        if (abs(dx) > abs(dy)) {
                            if (dx < -swipeThreshold) currentNext() else if (dx > swipeThreshold) currentPrevious()
                        } else {
                            if (dy < -swipeThreshold) queueVisible = true else if (dy > swipeThreshold) queueVisible = false
                        }
                    } else {
                        val now = System.currentTimeMillis()
                        if (now - lastTap < 320L) {
                            lastTap = 0L
                            currentTogglePlay()
                        } else {
                            lastTap = now
                        }
                    }
                    lastInteraction = System.currentTimeMillis()
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .burnInDrift(settings.burnInProtection && !reduceMotion),
        ) {
            if (state.currentTrack == null) {
                EmptyStage(Modifier.fillMaxSize())
            } else {
                when (settings.stageLayout) {
                    StageLayout.ART_LEFT -> ArtLeftLayout(
                        state = state,
                        settings = settings,
                        primary = primary,
                        secondary = secondary,
                        reduceMotion = reduceMotion,
                        modifier = Modifier.fillMaxSize(),
                    )

                    StageLayout.CENTERED_RING -> CenteredRingLayout(
                        state = state,
                        settings = settings,
                        primary = primary,
                        secondary = secondary,
                        reduceMotion = reduceMotion,
                        modifier = Modifier.fillMaxSize(),
                    )

                    StageLayout.TEXT_OVER_VISUALIZER -> TextOverVisualizerLayout(
                        state = state,
                        settings = settings,
                        primary = primary,
                        secondary = secondary,
                        reduceMotion = reduceMotion,
                        modifier = Modifier.fillMaxSize(),
                    )

                    StageLayout.VISUALIZER_ONLY -> Visualizer(
                        style = settings.visualizer,
                        modifier = Modifier.fillMaxSize(),
                        primary = primary,
                        secondary = secondary,
                        sensitivity = settings.visualizerSensitivity,
                    )
                }
            }
        }

        if (settings.stageLayout != StageLayout.VISUALIZER_ONLY) {
            StageProgressLine(
                positionMs = positionMs,
                durationMs = state.durationMs,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val progress = sweep.value
                    if (progress > 0f && progress < 1f) {
                        val bandWidth = size.width * 0.55f
                        val x = -bandWidth + (size.width + bandWidth * 2f) * progress
                        val brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                palette.secondary.copy(alpha = 0.25f),
                                palette.primary.copy(alpha = 0.20f),
                                Color.Transparent,
                            ),
                            startX = x,
                            endX = x + bandWidth,
                        )
                        rotate(degrees = -12f, pivot = Offset(size.width / 2f, size.height / 2f)) {
                            drawRect(
                                brush = brush,
                                topLeft = Offset(x, -size.height * 0.2f),
                                size = Size(bandWidth, size.height * 1.4f),
                            )
                        }
                    }
                },
        )

        DimOverlay(
            dimAfterMinutes = settings.dimAfterMinutes,
            lastInteractionMs = lastInteraction,
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(700)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    NeonIconButton(
                        icon = Icons.Rounded.FullscreenExit,
                        contentDescription = stringResource(R.string.stage_exit),
                        onClick = onExit,
                        size = 44.dp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.stage_exit_label),
                        style = NeonType.Label,
                        color = palette.textSecondary,
                    )
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(palette.base0.copy(alpha = 0.72f))
                        .border(1.dp, palette.border, RoundedCornerShape(36.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NeonIconButton(
                        icon = Icons.Rounded.SkipPrevious,
                        contentDescription = stringResource(R.string.np_previous),
                        onClick = onPrevious,
                        size = 42.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    PlayPauseButton(
                        isPlaying = state.isPlaying,
                        onClick = onTogglePlay,
                        size = 56.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    NeonIconButton(
                        icon = Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.np_next),
                        onClick = onNext,
                        size = 42.dp,
                    )
                    Spacer(Modifier.width(20.dp))
                    SeekBar(
                        positionMs = positionMs(),
                        durationMs = state.durationMs,
                        onSeek = onSeek,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = queueVisible,
            enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 3 },
            exit = fadeOut(tween(250)) + slideOutVertically(tween(250)) { it / 3 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            QueuePeek(upNext = upNext, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun EmptyStage(modifier: Modifier = Modifier) {
    val palette = Neon.palette
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SectionLabel(text = stringResource(R.string.label_nothing_playing), dotColor = palette.secondary)
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.stage_pick_track),
            style = NeonType.BodySmall,
            color = palette.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ArtLeftLayout(
    state: PlayerUiState,
    settings: AppSettings,
    primary: Color,
    secondary: Color,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        val artSize = minOf(maxHeight * 0.70f, maxWidth * 0.40f)
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(0.42f),
                contentAlignment = Alignment.Center,
            ) {
                FloatingArtCard(
                    track = state.currentTrack,
                    artSize = artSize,
                    reduceMotion = reduceMotion,
                )
            }
            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .padding(start = 28.dp),
            ) {
                SectionLabel(text = stringResource(R.string.label_now_playing), dotColor = secondary)
                Spacer(Modifier.height(10.dp))
                TrackTextBlock(
                    track = state.currentTrack,
                    settings = settings,
                    reduceMotion = reduceMotion,
                )
                Spacer(Modifier.height(18.dp))
                Visualizer(
                    style = settings.visualizer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    primary = primary,
                    secondary = secondary,
                    sensitivity = settings.visualizerSensitivity,
                )
            }
        }
    }
}

@Composable
private fun CenteredRingLayout(
    state: PlayerUiState,
    settings: AppSettings,
    primary: Color,
    secondary: Color,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.padding(20.dp)) {
        val minDim = minOf(maxWidth, maxHeight)
        val ringSize = minOf(minDim * 0.80f, maxHeight * 0.68f)
        val artSize = ringSize * 0.50f
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(ringSize),
                contentAlignment = Alignment.Center,
            ) {
                Visualizer(
                    style = VisualizerStyle.RADIAL,
                    modifier = Modifier.fillMaxSize(),
                    primary = primary,
                    secondary = secondary,
                    sensitivity = settings.visualizerSensitivity,
                    innerRadiusFraction = 0.55f,
                )
                Crossfade(
                    targetState = state.currentTrack,
                    animationSpec = tween(600),
                    label = "ringArt",
                ) { track ->
                    ArtImage(
                        track = track,
                        modifier = Modifier.size(artSize),
                        cornerRadius = artSize / 2,
                        glow = true,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            TrackTextBlock(
                track = state.currentTrack,
                settings = settings,
                reduceMotion = reduceMotion,
                centered = true,
            )
        }
    }
}

@Composable
private fun TextOverVisualizerLayout(
    state: PlayerUiState,
    settings: AppSettings,
    primary: Color,
    secondary: Color,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Visualizer(
            style = settings.visualizer,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.8f),
            primary = primary,
            secondary = secondary,
            sensitivity = settings.visualizerSensitivity,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            TrackTextBlock(
                track = state.currentTrack,
                settings = settings,
                reduceMotion = reduceMotion,
                centered = true,
            )
        }
    }
}

@Composable
private fun FloatingArtCard(
    track: Track?,
    artSize: Dp,
    reduceMotion: Boolean,
) {
    val palette = Neon.palette
    val density = LocalDensity.current
    val floatOffset = if (reduceMotion) {
        0.dp
    } else {
        val transition = rememberInfiniteTransition(label = "artFloat")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 6000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "artFloatPhase",
        )
        (phase * 8f - 4f).dp
    }
    Box(
        modifier = Modifier
            .size(artSize)
            .graphicsLayer {
                translationY = with(density) { floatOffset.toPx() }
            },
    ) {
        ArtImage(
            track = track,
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 18.dp,
            glow = true,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .drawWithCache {
                    val w = size.width
                    val h = size.height
                    val cr = 18.dp.toPx()
                    val specular = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                        startY = 0f,
                        endY = h * 0.42f,
                    )
                    val rim = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.30f), palette.primary.copy(alpha = 0.20f)),
                    )
                    val strokeWidth = 1.dp.toPx()
                    onDrawBehind {
                        drawRoundRect(
                            brush = specular,
                            topLeft = Offset(w * 0.02f, h * 0.02f),
                            size = Size(w * 0.96f, h * 0.40f),
                            cornerRadius = CornerRadius(cr, cr),
                        )
                        drawRoundRect(
                            brush = rim,
                            cornerRadius = CornerRadius(cr, cr),
                            style = Stroke(width = strokeWidth),
                        )
                    }
                },
        )
    }
}

@Composable
private fun TrackTextBlock(
    track: Track?,
    settings: AppSettings,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    centered: Boolean = false,
) {
    val palette = Neon.palette
    AnimatedContent(
        targetState = track,
        contentKey = { it?.id },
        transitionSpec = {
            if (reduceMotion) {
                fadeIn(tween(300)) togetherWith fadeOut(tween(300))
            } else {
                (fadeIn(tween(600)) + slideInHorizontally(tween(600)) { it / 6 }) togetherWith
                    (fadeOut(tween(600)) + slideOutHorizontally(tween(600)) { -it / 6 })
            }
        },
        label = "trackText",
        modifier = modifier,
    ) { current ->
        Column(
            horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            Text(
                text = current?.title.orEmpty(),
                style = NeonType.TitleXL,
                color = palette.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = current?.displayArtist.orEmpty(),
                style = NeonType.TitleM,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            )
            if (settings.stageShowAlbum) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = current?.displayAlbum.orEmpty(),
                    style = NeonType.Body,
                    color = palette.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                )
            }
        }
    }
}

@Composable
private fun QueuePeek(upNext: List<QueueEntry>, modifier: Modifier = Modifier) {
    val palette = Neon.palette
    NeonCard(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 96.dp),
    ) {
        SectionLabel(text = stringResource(R.string.label_up_next), dotColor = palette.secondary)
        Spacer(Modifier.height(8.dp))
        val entries = upNext.take(5)
        if (entries.isEmpty()) {
            Text(
                text = stringResource(R.string.stage_queue_empty),
                style = NeonType.BodySmall,
                color = palette.textMuted,
            )
        } else {
            entries.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = (entry.index + 1).toString().padStart(2, '0'),
                        style = NeonType.MonoSmall,
                        color = palette.textMuted,
                        modifier = Modifier.width(30.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.track.title,
                            style = NeonType.Body,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = entry.track.displayArtist,
                            style = NeonType.BodySmall,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = formatDuration(entry.track.durationMs),
                        style = NeonType.MonoSmall,
                        color = palette.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun StageProgressLine(
    positionMs: () -> Long,
    durationMs: Long,
    modifier: Modifier = Modifier,
) {
    val palette = Neon.palette
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(250)
            tick += 1L
        }
    }
    Canvas(modifier = modifier) {
        tick
        val duration = durationMs.coerceAtLeast(1L)
        val fraction = (positionMs().toFloat() / duration).coerceIn(0f, 1f)
        val h = size.height
        val cy = h / 2f
        drawLine(
            color = palette.textMuted.copy(alpha = 0.25f),
            start = Offset(0f, cy),
            end = Offset(size.width, cy),
            strokeWidth = h,
        )
        val x = size.width * fraction
        drawLine(
            color = palette.primary.copy(alpha = 0.8f),
            start = Offset(0f, cy),
            end = Offset(x, cy),
            strokeWidth = h,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.secondary.copy(alpha = 0.45f), Color.Transparent),
                center = Offset(x, cy),
                radius = h * 4f,
            ),
            radius = h * 4f,
            center = Offset(x, cy),
        )
        drawCircle(
            color = palette.secondary,
            radius = h * 1.6f,
            center = Offset(x, cy),
        )
    }
}
