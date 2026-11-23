package com.neonplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.neonplayer.data.art.ArtModel
import com.neonplayer.domain.model.Track
import com.neonplayer.ui.theme.Neon

@Composable
fun ArtImage(
    track: Track?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    glow: Boolean = false,
    sizePx: Int = 512,
) {
    val palette = Neon.palette
    val context = LocalContext.current
    val shape = RoundedCornerShape(cornerRadius)
    var state by remember(track?.uri) {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }
    Box(
        modifier = modifier
            .then(
                if (glow) {
                    Modifier.neonGlow(palette.primary, radius = 18.dp, cornerRadius = cornerRadius, alpha = 0.30f)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(palette.base1)
            .border(1.dp, palette.border, shape),
    ) {
        if (track != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(ArtModel(track.uri))
                    .size(sizePx)
                    .crossfade(250)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onState = { state = it },
            )
        }
        if (track == null || state !is AsyncImagePainter.State.Success) {
            ArtPlaceholder(Modifier.fillMaxSize())
        }
    }
}

@Composable
internal fun ArtPlaceholder(modifier: Modifier = Modifier) {
    val palette = Neon.palette
    Box(
        modifier = modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val step = (w / 8f).coerceAtLeast(8f)
            val background = Brush.verticalGradient(listOf(palette.base1, palette.base0))
            val grid = palette.primary.copy(alpha = 0.05f)
            val ring = palette.primary.copy(alpha = 0.35f)
            val bar = palette.secondary.copy(alpha = 0.55f)
            onDrawBehind {
                drawRect(brush = background)
                var x = step
                while (x < w) {
                    drawLine(grid, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                    x += step
                }
                var y = step
                while (y < h) {
                    drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                    y += step
                }
                val cx = w / 2f
                val cy = h * 0.44f
                val r = minOf(w, h) * 0.16f
                drawCircle(
                    color = ring,
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx()),
                )
                drawLine(ring, Offset(cx - r * 1.5f, cy), Offset(cx + r * 1.5f, cy), strokeWidth = 1.dp.toPx())
                drawLine(ring, Offset(cx, cy - r * 1.5f), Offset(cx, cy + r * 1.5f), strokeWidth = 1.dp.toPx())
                val barW = (w * 0.035f).coerceAtLeast(2f)
                val gap = barW * 0.9f
                val heights = floatArrayOf(0.35f, 0.6f, 0.45f, 0.75f)
                val totalW = heights.size * barW + (heights.size - 1) * gap
                var bx = cx - totalW / 2f
                val baseY = cy + r * 1.9f
                for (i in heights.indices) {
                    val bh = r * 0.9f * heights[i]
                    drawRoundRect(
                        color = bar,
                        topLeft = Offset(bx, baseY - bh),
                        size = Size(barW, bh),
                        cornerRadius = CornerRadius(barW / 2f, barW / 2f),
                    )
                    bx += barW + gap
                }
            }
        },
    )
}
