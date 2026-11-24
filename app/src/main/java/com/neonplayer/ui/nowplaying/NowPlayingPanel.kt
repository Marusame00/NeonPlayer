package com.neonplayer.ui.nowplaying

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimationRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.neonplayer.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neonplayer.domain.model.RepeatMode
import com.neonplayer.domain.model.VisualizerStyle
import com.neonplayer.domain.model.formatDuration
import com.neonplayer.playback.PlayerUiState
import com.neonplayer.playback.QueueEntry
import com.neonplayer.playback.SleepTimerState
import com.neonplayer.ui.components.ArtImage
import com.neonplayer.ui.components.NeonCard
import com.neonplayer.ui.components.NeonIconButton
import com.neonplayer.ui.components.NeonSlider
import com.neonplayer.ui.components.PillBadge
import com.neonplayer.ui.components.PlayPauseButton
import com.neonplayer.ui.components.SeekBar
import com.neonplayer.ui.components.SectionLabel
import com.neonplayer.ui.main.MainViewModel
import com.neonplayer.ui.theme.Neon
import com.neonplayer.ui.theme.NeonShapes
import com.neonplayer.ui.theme.NeonType
import com.neonplayer.ui.visualizer.Visualizer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyColumnState

@Composable
fun NowPlayingPanel(
    vm: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val player by vm.player.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val upNext by vm.upNext.collectAsStateWithLifecycle()
    val sleepTimer by vm.sleepTimer.collectAsStateWithLifecycle()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxHeight < 560.dp && maxWidth >= 440.dp) {
            val compactArt = (maxHeight - 250.dp).coerceIn(72.dp, 150.dp)
            Row(modifier = Modifier.fillMaxSize()) {
                CompactNowPlayingCard(
                    vm = vm,
                    player = player,
                    volume = settings.volume,
                    artSize = compactArt,
                    visualizerStyle = settings.visualizer,
                    visualizerSensitivity = settings.visualizerSensitivity,
                    sleepTimer = sleepTimer,
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxSize(),
                )
                Spacer(Modifier.width(10.dp))
                UpNextCard(
                    vm = vm,
                    upNext = upNext,
                    shuffle = player.shuffle,
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxSize(),
                )
            }
            return@BoxWithConstraints
        }
        val artSize = (maxHeight * 0.20f).coerceIn(132.dp, 168.dp)
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                NowPlayingHeader(
                    player = player,
                    artSize = artSize,
                    visualizerStyle = settings.visualizer,
                    visualizerSensitivity = settings.visualizerSensitivity,
                    sleepTimer = sleepTimer,
                )
                Spacer(Modifier.height(8.dp))
                SeekBarHost(vm = vm, durationMs = player.durationMs)
                Spacer(Modifier.height(4.dp))
                TransportRow(vm = vm, player = player)
                Spacer(Modifier.height(6.dp))
                VolumeRow(vm = vm, volume = settings.volume, sleepTimer = sleepTimer)
            }
            Spacer(Modifier.height(8.dp))
            UpNextCard(
                vm = vm,
                upNext = upNext,
                shuffle = player.shuffle,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NowPlayingHeader(
    player: PlayerUiState,
    artSize: Dp,
    visualizerStyle: VisualizerStyle,
    visualizerSensitivity: Float,
    sleepTimer: SleepTimerState,
) {
    val palette = Neon.palette
    val track = player.currentTrack
    NeonCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            ArtImage(
                track = track,
                modifier = Modifier.size(artSize),
                cornerRadius = 14.dp,
                glow = true,
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulsingDot(playing = player.isPlaying)
                    Spacer(Modifier.width(7.dp))
                    SectionLabel(text = stringResource(R.string.label_now_playing), color = palette.secondary)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = track?.title ?: stringResource(R.string.label_nothing_playing),
                    style = NeonType.TitleL,
                    color = palette.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = track?.displayArtist.orEmpty(),
                    style = NeonType.TitleM,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = track?.displayAlbum.orEmpty(),
                    style = NeonType.BodySmall,
                    color = palette.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (track != null && track.format.isNotBlank()) {
                        PillBadge(text = track.format)
                    }
                    PillBadge(text = formatDuration(player.durationMs), color = palette.secondary)
                    SleepBadge(state = sleepTimer)
                }
                Spacer(Modifier.height(8.dp))
                Visualizer(
                    style = visualizerStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    sensitivity = visualizerSensitivity,
                )
            }
        }
    }
}

@Composable
private fun CompactNowPlayingCard(
    vm: MainViewModel,
    player: PlayerUiState,
    volume: Float,
    artSize: Dp,
    visualizerStyle: VisualizerStyle,
    visualizerSensitivity: Float,
    sleepTimer: SleepTimerState,
    modifier: Modifier = Modifier,
) {
    val palette = Neon.palette
    val track = player.currentTrack
    NeonCard(modifier = modifier, contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            ArtImage(
                track = track,
                modifier = Modifier.size(artSize),
                cornerRadius = 14.dp,
                glow = true,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulsingDot(playing = player.isPlaying)
                    Spacer(Modifier.width(7.dp))
                    SectionLabel(text = stringResource(R.string.label_now_playing), color = palette.secondary)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = track?.title ?: stringResource(R.string.label_nothing_playing),
                    style = NeonType.TitleL,
                    color = palette.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track?.displayArtist.orEmpty(),
                    style = NeonType.TitleM,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track?.displayAlbum.orEmpty(),
                    style = NeonType.BodySmall,
                    color = palette.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (track != null && track.format.isNotBlank()) {
                        PillBadge(text = track.format)
                    }
                    PillBadge(text = formatDuration(player.durationMs), color = palette.secondary)
                    SleepBadge(state = sleepTimer)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Visualizer(
            style = visualizerStyle,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            sensitivity = visualizerSensitivity,
        )
        SeekBarHost(vm = vm, durationMs = player.durationMs)
        TransportRow(vm = vm, player = player, compact = true)
        Spacer(Modifier.height(4.dp))
        VolumeRow(vm = vm, volume = volume, sleepTimer = sleepTimer)
    }
}

@Composable
private fun PulsingDot(playing: Boolean) {
    val palette = Neon.palette
    val reduceMotion = Neon.style.reduceMotion
    val alpha = if (playing && !reduceMotion) {
        val transition = rememberInfiniteTransition(label = "nowPlayingDot")
        val value by transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = LinearEasing),
                repeatMode = AnimationRepeatMode.Reverse,
            ),
            label = "nowPlayingDotAlpha",
        )
        value
    } else {
        1f
    }
    Box(
        Modifier
            .size(6.dp)
            .graphicsLayer { this.alpha = alpha }
            .background(palette.secondary, CircleShape),
    )
}

@Composable
private fun SleepBadge(state: SleepTimerState) {
    val palette = Neon.palette
    val active = state.endsAtMs != null || state.endOfTrack
    if (!active) return
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.endsAtMs, state.endOfTrack) {
        while (isActive) {
            now = System.currentTimeMillis()
            delay(15_000L)
        }
    }
    val label = if (state.endOfTrack) {
        stringResource(R.string.np_sleep_badge_eot)
    } else {
        val remaining = ((state.endsAtMs ?: now) - now).coerceAtLeast(0L)
        stringResource(
            R.string.np_sleep_badge_min,
            (remaining / 60_000L).coerceAtLeast(1L),
        )
    }
    PillBadge(text = label, color = palette.tertiary)
}

@Composable
private fun SeekBarHost(vm: MainViewModel, durationMs: Long) {
    val position by vm.positionMs.collectAsStateWithLifecycle()
    SeekBar(
        positionMs = position,
        durationMs = durationMs,
        onSeek = vm::seekTo,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TransportRow(vm: MainViewModel, player: PlayerUiState, compact: Boolean = false) {
    val baseEdge = if (compact) 36.dp else 40.dp
    val baseMid = if (compact) 40.dp else 44.dp
    val basePlay = if (compact) 56.dp else 68.dp
    val baseGap = if (compact) 4.dp else 6.dp
    val basePlayGap = if (compact) 8.dp else 10.dp
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val needed = with(density) {
            (baseEdge * 2 + baseMid * 3 + basePlay + baseGap * 3 + basePlayGap * 2).toPx()
        }
        val available = with(density) { maxWidth.toPx() }
        val scale = if (needed > available && needed > 0f) {
            (available / needed).coerceIn(0.62f, 1f)
        } else {
            1f
        }
        val edge = baseEdge * scale
        val mid = baseMid * scale
        val play = basePlay * scale
        val gap = baseGap * scale
        val playGap = basePlayGap * scale
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeonIconButton(
                icon = Icons.Rounded.Shuffle,
                contentDescription = stringResource(R.string.np_shuffle),
                onClick = vm::toggleShuffle,
                size = edge,
                active = player.shuffle,
            )
            Spacer(Modifier.width(gap))
            NeonIconButton(
                icon = Icons.Rounded.SkipPrevious,
                contentDescription = stringResource(R.string.np_previous),
                onClick = vm::previous,
                size = mid,
            )
            Spacer(Modifier.width(playGap))
            PlayPauseButton(
                isPlaying = player.isPlaying,
                onClick = vm::togglePlayPause,
                size = play,
            )
            Spacer(Modifier.width(playGap))
            NeonIconButton(
                icon = Icons.Rounded.Stop,
                contentDescription = stringResource(R.string.np_stop),
                onClick = vm::stop,
                size = mid,
            )
            Spacer(Modifier.width(gap))
            NeonIconButton(
                icon = Icons.Rounded.SkipNext,
                contentDescription = stringResource(R.string.np_next),
                onClick = vm::next,
                size = mid,
            )
            Spacer(Modifier.width(gap))
            NeonIconButton(
                icon = if (player.repeat == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = stringResource(R.string.np_repeat),
                onClick = vm::cycleRepeat,
                size = edge,
                active = player.repeat != RepeatMode.OFF,
            )
        }
    }
}

@Composable
private fun VolumeRow(vm: MainViewModel, volume: Float, sleepTimer: SleepTimerState) {
    val palette = Neon.palette
    var sleepMenu by remember { mutableStateOf(false) }
    val sleepActive = sleepTimer.endsAtMs != null || sleepTimer.endOfTrack
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = when {
                volume <= 0.001f -> Icons.Rounded.VolumeOff
                volume < 0.5f -> Icons.Rounded.VolumeDown
                else -> Icons.Rounded.VolumeUp
            },
            contentDescription = stringResource(R.string.np_volume),
            tint = palette.textSecondary,
            modifier = Modifier.size(18.dp),
        )
        NeonSlider(
            value = volume,
            onValueChange = vm::setVolume,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${(volume * 100f).toInt()}%",
            style = NeonType.MonoSmall,
            color = palette.primary,
            modifier = Modifier.width(38.dp),
            textAlign = TextAlign.End,
        )
        Box {
            NeonIconButton(
                icon = Icons.Rounded.Bedtime,
                contentDescription = stringResource(R.string.np_sleep),
                onClick = { sleepMenu = true },
                size = 34.dp,
                active = sleepActive,
            )
            DropdownMenu(
                expanded = sleepMenu,
                onDismissRequest = { sleepMenu = false },
                containerColor = palette.surfaceHigh,
                shape = RoundedCornerShape(NeonShapes.SmallRadius),
            ) {
                SleepEntry(stringResource(R.string.action_off)) {
                    sleepMenu = false
                    vm.cancelSleepTimer()
                }
                SLEEP_MINUTES.forEach { minutes ->
                    SleepEntry(stringResource(R.string.settings_sleep_min, minutes)) {
                        sleepMenu = false
                        vm.startSleepTimer(minutes)
                    }
                }
                SleepEntry(stringResource(R.string.settings_sleep_end_of_track)) {
                    sleepMenu = false
                    vm.startSleepTimerEndOfTrack()
                }
            }
        }
    }
}

@Composable
private fun SleepEntry(label: String, onClick: () -> Unit) {
    val palette = Neon.palette
    Text(
        text = label,
        style = NeonType.Body,
        color = palette.textPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun UpNextCard(
    vm: MainViewModel,
    upNext: List<QueueEntry>,
    shuffle: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = Neon.palette
    val listState = rememberLazyListState()
    val items = remember(upNext) { upNext.toMutableStateList() }
    val reorderState = rememberReorderableLazyColumnState(listState) { from, to ->
        val fromEntry = items.getOrNull(from.index)
        val toEntry = items.getOrNull(to.index)
        if (fromEntry != null && toEntry != null) {
            vm.moveQueueItem(fromEntry.index, toEntry.index)
            items.add(to.index, items.removeAt(from.index))
        }
    }

    NeonCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionLabel(text = stringResource(R.string.label_up_next))
            Text(
                text = stringResource(R.string.np_queued, upNext.size),
                style = NeonType.MonoSmall,
                color = palette.textMuted,
            )
            Spacer(Modifier.weight(1f))
            NeonIconButton(
                icon = Icons.Rounded.ClearAll,
                contentDescription = stringResource(R.string.np_clear_queue),
                onClick = vm::clearUpNext,
                size = 30.dp,
                enabled = upNext.isNotEmpty(),
            )
        }
        Spacer(Modifier.height(4.dp))
        if (upNext.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.np_queue_empty),
                    style = NeonType.BodySmall,
                    color = palette.textMuted,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            if (shuffle) {
                Text(
                    text = stringResource(R.string.np_queue_shuffle_off),
                    style = NeonType.MonoSmall,
                    color = palette.textMuted,
                )
                Spacer(Modifier.height(2.dp))
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 2.dp),
            ) {
                itemsIndexed(
                    items = items,
                    key = { _, entry -> entry.key },
                    contentType = { _, _ -> "queueEntry" },
                ) { position, entry ->
                    ReorderableItem(
                        state = reorderState,
                        key = entry.key,
                        enabled = !shuffle,
                    ) { _ ->
                        QueueRow(
                            entry = entry,
                            position = position,
                            onSkip = { vm.skipToIndex(entry.index) },
                            onRemove = { vm.removeQueueItem(entry.index) },
                            handle = {
                                Box(Modifier.draggableHandle(enabled = !shuffle)) {
                                    Icon(
                                        imageVector = Icons.Rounded.DragIndicator,
                                        contentDescription = stringResource(R.string.np_reorder),
                                        tint = palette.textMuted,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    entry: QueueEntry,
    position: Int,
    onSkip: () -> Unit,
    onRemove: () -> Unit,
    handle: @Composable () -> Unit,
) {
    val palette = Neon.palette
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onRemove()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.secondary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ClearAll,
                    contentDescription = null,
                    tint = palette.secondary,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(16.dp),
                )
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(palette.surface.copy(alpha = 0.92f))
                .clickable(onClick = onSkip)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = (position + 1).toString().padStart(2, '0'),
                style = NeonType.MonoSmall,
                color = palette.textMuted,
                modifier = Modifier.width(22.dp),
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
            Text(
                text = formatDuration(entry.track.durationMs),
                style = NeonType.MonoSmall,
                color = palette.textSecondary,
            )
            handle()
        }
    }
}

private val SLEEP_MINUTES = listOf(15, 30, 45, 60, 90)
