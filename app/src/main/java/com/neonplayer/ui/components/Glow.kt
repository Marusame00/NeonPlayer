package com.neonplayer.ui.components

import android.graphics.Paint
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neonplayer.ui.theme.NeonColors

fun Modifier.neonGlow(
    color: Color,
    radius: Dp = 12.dp,
    cornerRadius: Dp = 16.dp,
    alpha: Float = 0.35f,
): Modifier = drawBehind {
    val r = radius.toPx()
    val cr = cornerRadius.toPx()
    val a = alpha.coerceIn(0f, 1f)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        drawIntoCanvas { canvas ->
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = android.graphics.Color.TRANSPARENT
            paint.setShadowLayer(r, 0f, 0f, color.copy(alpha = a).toArgb())
            canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, cr, cr, paint)
        }
    } else {
        val stroke = 1.5.dp.toPx()
        for (i in 3 downTo 1) {
            val grow = r * (i / 3f) * 0.5f
            drawRoundRect(
                color = color.copy(alpha = a * (0.30f / i)),
                topLeft = Offset(-grow, -grow),
                size = Size(size.width + grow * 2f, size.height + grow * 2f),
                cornerRadius = CornerRadius(cr + grow, cr + grow),
                style = Stroke(width = stroke),
            )
        }
    }
}

fun Modifier.gridTexture(
    color: Color = NeonColors.Cyan.copy(alpha = 0.045f),
    step: Dp = 28.dp,
): Modifier = drawWithCache {
    val stepPx = step.toPx().coerceAtLeast(1f)
    val cols = (size.width / stepPx).toInt() + 1
    val rows = (size.height / stepPx).toInt() + 1
    val topGlow = Brush.radialGradient(
        colors = listOf(color.copy(alpha = (color.alpha * 1.4f).coerceAtMost(0.08f)), Color.Transparent),
        center = Offset(size.width * 0.18f, size.height * 0.12f),
        radius = size.maxDimension * 0.7f,
    )
    val bottomGlow = Brush.radialGradient(
        colors = listOf(color.copy(alpha = (color.alpha * 0.9f).coerceAtMost(0.06f)), Color.Transparent),
        center = Offset(size.width * 0.85f, size.height * 0.9f),
        radius = size.maxDimension * 0.6f,
    )
    onDrawBehind {
        drawRect(brush = topGlow)
        drawRect(brush = bottomGlow)
        for (i in 0..cols) {
            val x = i * stepPx
            drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        }
        for (j in 0..rows) {
            val y = j * stepPx
            drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
    }
}
