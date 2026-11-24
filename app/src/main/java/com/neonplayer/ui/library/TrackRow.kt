package com.neonplayer.ui.library

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.neonplayer.R
import com.neonplayer.domain.model.Track
import com.neonplayer.domain.model.formatDuration
import com.neonplayer.ui.components.ArtImage
import com.neonplayer.ui.components.PillBadge
import com.neonplayer.ui.theme.Neon
import com.neonplayer.ui.theme.NeonType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun TrackRow(
    track: Track,
    index: Int,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    actions: TrackMenuActions,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val palette = Neon.palette
    var menuOpen by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isCurrent) palette.primary.copy(alpha = 0.10f) else Color.Transparent)
                .drawBehind {
                    if (isCurrent) {
                        drawRect(
                            color = palette.secondary,
                            topLeft = Offset.Zero,
                            size = Size(2.dp.toPx(), size.height),
                        )
                    }
                }
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuOpen = true },
                )
                .padding(start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier.width(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isCurrent) {
                    NowPlayingGlyph(playing = isPlaying)
                } else {
                    Text(
                        text = (index + 1).toString().padStart(2, '0'),
                        style = NeonType.MonoSmall,
                        color = palette.textMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            ArtImage(
                track = track,
                modifier = Modifier.size(40.dp),
                cornerRadius = 8.dp,
                sizePx = 128,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = NeonType.Body,
                    color = if (isCurrent) palette.primary else palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.displayArtist,
                    style = NeonType.BodySmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (track.format.isNotBlank()) {
                PillBadge(text = track.format, color = palette.textSecondary)
            }
            Text(
                text = formatDuration(track.durationMs),
                style = NeonType.MonoSmall,
                color = palette.textSecondary,
                textAlign = TextAlign.End,
                modifier = Modifier.width(42.dp),
            )
            if (trailing != null) {
                trailing()
            }
        }
        TrackContextMenu(
            expanded = menuOpen,
            onDismiss = { menuOpen = false },
            actions = actions,
        )
    }
}

@Composable
fun NowPlayingGlyph(
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = Neon.palette
    val reduceMotion = Neon.style.reduceMotion
    val transition = rememberInfiniteTransition(label = "rowGlyph")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rowGlyphPhase",
    )
    Canvas(modifier = modifier.size(width = 14.dp, height = 14.dp)) {
        val bars = 3
        val gap = size.width * 0.18f
        val barWidth = (size.width - gap * (bars - 1)) / bars
        for (i in 0 until bars) {
            val factor = if (playing && !reduceMotion) {
                val p = (phase + i * 0.33f) % 1f
                (0.30f + 0.70f * abs(sin(p * PI.toFloat()))).coerceIn(0.20f, 1f)
            } else {
                0.55f
            }
            val barHeight = size.height * factor
            drawRoundRect(
                color = palette.secondary,
                topLeft = Offset(i * (barWidth + gap), size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

@Composable
fun DragHandle(modifier: Modifier = Modifier) {
    val palette = Neon.palette
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.DragIndicator,
                    contentDescription = stringResource(R.string.track_drag_reorder),
            tint = palette.textMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}
