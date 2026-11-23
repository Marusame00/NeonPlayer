package com.neonplayer.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neonplayer.ui.components.ArtImage
import com.neonplayer.ui.components.NeonCard
import com.neonplayer.ui.components.NeonIconButton
import com.neonplayer.ui.library.LibraryPanel
import com.neonplayer.ui.nowplaying.NowPlayingPanel
import com.neonplayer.ui.theme.Neon
import com.neonplayer.ui.theme.NeonType

@Composable
fun PortraitMain(
    vm: MainViewModel,
    audioPermissionGranted: Boolean,
    onGrantAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    BackHandler(enabled = expanded) { expanded = false }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
        ) {
            NeonTopBar(vm = vm, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            LibraryPanel(
                vm = vm,
                audioPermissionGranted = audioPermissionGranted,
                onGrantAccess = onGrantAccess,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            MiniPlayer(vm = vm, onExpand = { expanded = true })
        }
        NowPlayingSheet(vm = vm, visible = expanded, onClose = { expanded = false })
    }
}

@Composable
private fun MiniPlayer(
    vm: MainViewModel,
    onExpand: () -> Unit,
) {
    val palette = Neon.palette
    val player by vm.player.collectAsStateWithLifecycle()
    val track = player.currentTrack
    NeonCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        contentPadding = PaddingValues(0.dp),
        glow = false,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onExpand),
        ) {
            MiniProgress(
                vm = vm,
                durationMs = player.durationMs,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(2.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ArtImage(
                    track = track,
                    modifier = Modifier.size(44.dp),
                    cornerRadius = 8.dp,
                    sizePx = 128,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track?.title ?: "Nothing playing",
                        style = NeonType.Body,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track?.displayArtist.orEmpty(),
                        style = NeonType.BodySmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                NeonIconButton(
                    icon = if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (player.isPlaying) "Pause" else "Play",
                    onClick = vm::togglePlayPause,
                    size = 44.dp,
                    active = player.isPlaying,
                )
                NeonIconButton(
                    icon = Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    onClick = vm::next,
                    size = 36.dp,
                )
            }
        }
    }
}

@Composable
private fun MiniProgress(
    vm: MainViewModel,
    durationMs: Long,
    modifier: Modifier = Modifier,
) {
    val palette = Neon.palette
    val position by vm.positionMs.collectAsStateWithLifecycle()
    val fraction = if (durationMs > 0L) (position.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Box(
        modifier = modifier.background(palette.textMuted.copy(alpha = 0.20f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(
                    Brush.horizontalGradient(
                        listOf(palette.primary.copy(alpha = 0.6f), palette.primary),
                    ),
                ),
        )
    }
}

@Composable
private fun NowPlayingSheet(
    vm: MainViewModel,
    visible: Boolean,
    onClose: () -> Unit,
) {
    val palette = Neon.palette
    val player by vm.player.collectAsStateWithLifecycle()
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(320)) { it } + fadeIn(tween(200)),
        exit = slideOutVertically(tween(260)) { it } + fadeOut(tween(180)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.base0.copy(alpha = 0.98f))
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NeonIconButton(
                        icon = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Close now playing",
                        onClick = onClose,
                        size = 34.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "NOW PLAYING",
                        style = NeonType.Label,
                        color = palette.secondary,
                    )
                    Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                ArtImage(
                    track = player.currentTrack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp)),
                    cornerRadius = 18.dp,
                    glow = true,
                    sizePx = 768,
                )
                Spacer(Modifier.height(8.dp))
                NowPlayingPanel(
                    vm = vm,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            }
        }
    }
}
