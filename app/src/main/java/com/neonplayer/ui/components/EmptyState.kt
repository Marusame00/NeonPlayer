package com.neonplayer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neonplayer.ui.theme.Neon
import com.neonplayer.ui.theme.NeonType

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    val palette = Neon.palette
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmptyStateArt()
        Spacer(Modifier.height(18.dp))
        Text(
            text = title,
            style = NeonType.TitleM,
            color = palette.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = NeonType.BodySmall,
            color = palette.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeonTextButton(text = actionLabel, onClick = onAction, primary = true)
                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    NeonTextButton(text = secondaryActionLabel, onClick = onSecondaryAction)
                }
            }
        }
    }
}

@Composable
private fun EmptyStateArt(modifier: Modifier = Modifier) {
    val palette = Neon.palette
    Canvas(modifier = modifier.size(width = 120.dp, height = 72.dp)) {
        val w = size.width
        val h = size.height
        val horizon = h * 0.62f
        val sunRadius = w * 0.24f
        val sunCenter = Offset(w / 2f, horizon)
        val sunPath = Path().apply {
            addOval(
                androidx.compose.ui.geometry.Rect(
                    center = sunCenter,
                    radius = sunRadius,
                ),
            )
        }
        val sunBrush = Brush.verticalGradient(
            colors = listOf(palette.secondary, palette.tertiary),
            startY = horizon - sunRadius,
            endY = horizon,
        )
        clipPath(sunPath) {
            val bands = 7
            var y = horizon - sunRadius
            for (i in 0 until bands) {
                val bandHeight = sunRadius * 0.10f
                val gap = sunRadius * 0.012f * i
                drawRect(
                    brush = sunBrush,
                    topLeft = Offset(0f, y),
                    size = Size(w, bandHeight),
                )
                y += bandHeight + gap
            }
        }
        drawLine(
            color = palette.primary.copy(alpha = 0.55f),
            start = Offset(0f, horizon),
            end = Offset(w, horizon),
            strokeWidth = 1.5.dp.toPx(),
        )
        val gridColor = palette.primary.copy(alpha = 0.35f)
        val rows = 4
        for (i in 1..rows) {
            val f = i / rows.toFloat()
            val y = horizon + (h - horizon) * f * f
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }
        val vanishing = Offset(w / 2f, horizon)
        for (i in -3..3) {
            val x = w / 2f + i * w * 0.16f
            drawLine(
                color = gridColor,
                start = vanishing,
                end = Offset(x, h),
                strokeWidth = 1f,
            )
        }
        drawCircle(
            color = palette.secondary.copy(alpha = 0.18f),
            radius = sunRadius * 1.25f,
            center = sunCenter,
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}
