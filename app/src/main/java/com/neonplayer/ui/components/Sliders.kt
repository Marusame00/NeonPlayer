package com.neonplayer.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neonplayer.domain.model.formatDuration
import com.neonplayer.ui.theme.Neon
import com.neonplayer.ui.theme.NeonType

@Composable
fun NeonSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    color: Color = Neon.palette.primary,
    enabled: Boolean = true,
    thumbRadius: Dp = 7.dp,
    trackHeight: Dp = 4.dp,
) {
    val palette = Neon.palette
    var dragging by remember { mutableStateOf(false) }
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(enabled, valueRange) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    dragging = true
                    fun emit(x: Float) {
                        val f = (x / width).coerceIn(0f, 1f)
                        onValueChange(valueRange.start + f * span)
                    }
                    emit(down.position.x)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        emit(change.position.x)
                        change.consume()
                    }
                    dragging = false
                    onValueChangeFinished?.invoke()
                }
            },
    ) {
        val cy = size.height / 2f
        val r = thumbRadius.toPx()
        val th = trackHeight.toPx()
        val inset = r + 2.dp.toPx()
        val trackWidth = (size.width - inset * 2f).coerceAtLeast(1f)
        val thumbX = inset + trackWidth * fraction
        val radius = if (dragging) r * 1.15f else r
        val trackColor = palette.textMuted.copy(alpha = 0.25f)
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(inset, cy - th / 2f),
            size = Size(trackWidth, th),
            cornerRadius = CornerRadius(th / 2f, th / 2f),
        )
        if (fraction > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(color.copy(alpha = 0.55f), color),
                    startX = inset,
                    endX = thumbX.coerceAtLeast(inset + 1f),
                ),
                topLeft = Offset(inset, cy - th / 2f),
                size = Size((thumbX - inset).coerceAtLeast(th), th),
                cornerRadius = CornerRadius(th / 2f, th / 2f),
            )
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.30f), Color.Transparent),
                center = Offset(thumbX, cy),
                radius = radius * 2.6f,
            ),
            radius = radius * 2.6f,
            center = Offset(thumbX, cy),
        )
        drawCircle(
            color = lerp(color, palette.textPrimary, 0.35f),
            radius = radius,
            center = Offset(thumbX, cy),
        )
        drawCircle(
            color = color.copy(alpha = 0.9f),
            radius = radius,
            center = Offset(thumbX, cy),
            style = Stroke(width = 1.5.dp.toPx()),
        )
    }
}

@Composable
fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Neon.palette
    val duration = durationMs.coerceAtLeast(1L)
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val fraction = dragFraction ?: (positionMs.toFloat() / duration).coerceIn(0f, 1f)
    Column(modifier = modifier) {
        NeonSlider(
            value = fraction,
            onValueChange = { dragFraction = it },
            onValueChangeFinished = {
                val target = dragFraction
                if (target != null) onSeek((target * duration).toLong().coerceIn(0L, duration))
                dragFraction = null
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration((fraction * duration).toLong()),
                style = NeonType.MonoSmall,
                color = palette.primary,
            )
            Text(
                text = formatDuration(durationMs),
                style = NeonType.MonoSmall,
                color = palette.textSecondary,
            )
        }
    }
}

@Composable
fun NeonVerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    color: Color = Neon.palette.primary,
    centerLine: Boolean = true,
) {
    val palette = Neon.palette
    var dragging by remember { mutableStateOf(false) }
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    Canvas(
        modifier = modifier
            .defaultMinSize(minWidth = 32.dp, minHeight = 120.dp)
            .pointerInput(valueRange) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val height = size.height.toFloat().coerceAtLeast(1f)
                    dragging = true
                    fun emit(y: Float) {
                        val f = (1f - (y / height)).coerceIn(0f, 1f)
                        onValueChange(valueRange.start + f * span)
                    }
                    emit(down.position.y)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        emit(change.position.y)
                        change.consume()
                    }
                    dragging = false
                    onValueChangeFinished?.invoke()
                }
            },
    ) {
        val cx = size.width / 2f
        val r = 6.dp.toPx()
        val th = 4.dp.toPx()
        val inset = r + 2.dp.toPx()
        val trackHeight = (size.height - inset * 2f).coerceAtLeast(1f)
        val thumbY = inset + trackHeight * (1f - fraction)
        val radius = if (dragging) r * 1.15f else r
        val centerY = inset + trackHeight / 2f
        drawRoundRect(
            color = palette.textMuted.copy(alpha = 0.25f),
            topLeft = Offset(cx - th / 2f, inset),
            size = Size(th, trackHeight),
            cornerRadius = CornerRadius(th / 2f, th / 2f),
        )
        if (centerLine) {
            val top = minOf(thumbY, centerY)
            val bottom = maxOf(thumbY, centerY)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(color, color.copy(alpha = 0.55f)),
                    startY = top,
                    endY = bottom.coerceAtLeast(top + 1f),
                ),
                topLeft = Offset(cx - th / 2f, top),
                size = Size(th, (bottom - top).coerceAtLeast(th)),
                cornerRadius = CornerRadius(th / 2f, th / 2f),
            )
            drawLine(
                color = palette.textSecondary.copy(alpha = 0.35f),
                start = Offset(cx - 9.dp.toPx(), centerY),
                end = Offset(cx + 9.dp.toPx(), centerY),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        } else {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(color, color.copy(alpha = 0.55f)),
                    startY = thumbY,
                    endY = inset + trackHeight,
                ),
                topLeft = Offset(cx - th / 2f, thumbY),
                size = Size(th, (inset + trackHeight - thumbY).coerceAtLeast(th)),
                cornerRadius = CornerRadius(th / 2f, th / 2f),
            )
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.30f), Color.Transparent),
                center = Offset(cx, thumbY),
                radius = radius * 2.6f,
            ),
            radius = radius * 2.6f,
            center = Offset(cx, thumbY),
        )
        drawCircle(
            color = lerp(color, palette.textPrimary, 0.35f),
            radius = radius,
            center = Offset(cx, thumbY),
        )
        drawCircle(
            color = color.copy(alpha = 0.9f),
            radius = radius,
            center = Offset(cx, thumbY),
            style = Stroke(width = 1.5.dp.toPx()),
        )
    }
}

@Composable
fun NeonSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Neon.palette
    val reduceMotion = Neon.style.reduceMotion
    val offset by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "switchOffset",
    )
    Box(
        modifier = modifier
            .size(width = 46.dp, height = 26.dp)
            .clip(CircleShape)
            .background(if (checked) palette.primary.copy(alpha = 0.20f) else palette.textMuted.copy(alpha = 0.16f))
            .border(1.dp, if (checked) palette.primary.copy(alpha = 0.55f) else palette.border, CircleShape)
            .clickable { onCheckedChange(!checked) }
            .drawBehind {
                val pad = 3.dp.toPx()
                val r = size.height / 2f - pad
                val cy = size.height / 2f
                val cx = pad + r + (size.width - (pad + r) * 2f) * offset
                if (checked) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(palette.primary.copy(alpha = 0.35f), Color.Transparent),
                            center = Offset(cx, cy),
                            radius = r * 2.2f,
                        ),
                        radius = r * 2.2f,
                        center = Offset(cx, cy),
                    )
                }
                drawCircle(
                    color = if (checked) palette.primary else palette.textSecondary,
                    radius = r,
                    center = Offset(cx, cy),
                )
            },
    )
}
