package com.neonplayer.ui.visualizer

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.neonplayer.domain.model.VisualizerStyle
import com.neonplayer.playback.VisualizerBus
import com.neonplayer.ui.theme.Neon
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val BAR_COUNT = 48
private const val SMOOTHING = 0.35f
private const val PEAK_FALL = 0.008f

@Composable
fun Visualizer(
    style: VisualizerStyle,
    modifier: Modifier = Modifier,
    primary: Color = Neon.palette.primary,
    secondary: Color = Neon.palette.secondary,
    sensitivity: Float = 1f,
    innerRadiusFraction: Float = 0.62f,
    active: Boolean = true,
) {
    val reduceMotion = Neon.style.reduceMotion
    val bands = remember { FloatArray(VisualizerBus.BANDS) }
    val wave = remember { FloatArray(VisualizerBus.WAVE_SIZE) }
    val smoothBands = remember { FloatArray(BAR_COUNT) }
    val peaks = remember { FloatArray(BAR_COUNT) }
    val smoothWave = remember { FloatArray(VisualizerBus.WAVE_SIZE) }
    val bassSmooth = remember { FloatArray(1) }
    val gridPhase = remember { FloatArray(1) }
    val path = remember { Path() }
    var tick by remember { mutableLongStateOf(0L) }
    var lastFrameId by remember { mutableLongStateOf(-1L) }

    LaunchedEffect(sensitivity) {
        VisualizerBus.sensitivity = sensitivity
    }

    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        while (isActive) {
            withFrameNanos {
                val id = VisualizerBus.frameId
                if (id != lastFrameId) {
                    lastFrameId = id
                    VisualizerBus.readBands(bands)
                    VisualizerBus.readWave(wave)
                    for (i in 0 until BAR_COUNT) {
                        val band = bands[(i * VisualizerBus.BANDS) / BAR_COUNT]
                        smoothBands[i] += (band - smoothBands[i]) * SMOOTHING
                    }
                    for (i in smoothWave.indices) {
                        smoothWave[i] += (wave[i] - smoothWave[i]) * SMOOTHING
                    }
                }
                val bass = VisualizerBus.bass.coerceIn(0f, 1f)
                bassSmooth[0] += (bass - bassSmooth[0]) * SMOOTHING
                for (i in 0 until BAR_COUNT) {
                    val target = smoothBands[i]
                    peaks[i] = if (target > peaks[i]) target else (peaks[i] - PEAK_FALL).coerceAtLeast(target)
                }
                val speed = if (reduceMotion) 0.0012f else 0.0015f + bassSmooth[0] * 0.008f
                gridPhase[0] = (gridPhase[0] + speed) % 1f
                tick += 1L
            }
        }
    }

    Spacer(
        modifier = modifier.drawWithCache {
            val barGradient = Brush.verticalGradient(listOf(primary, secondary))
            val sweep = Brush.sweepGradient(listOf(primary, secondary, primary))
            val sunBrush = Brush.verticalGradient(
                colors = listOf(secondary, primary),
                startY = size.height * 0.29f,
                endY = size.height * 0.45f,
            )
            val glow = primary.copy(alpha = 0.10f)
            val centerLine = primary.copy(alpha = 0.35f)
            onDrawBehind {
                tick
                when (style) {
                    VisualizerStyle.BARS -> drawBars(
                        bands = smoothBands,
                        peaks = peaks,
                        barGradient = barGradient,
                        glow = glow,
                        peakColor = secondary,
                        reduceMotion = reduceMotion,
                    )

                    VisualizerStyle.MIRRORED -> drawMirrored(
                        bands = smoothBands,
                        barGradient = barGradient,
                        glow = glow,
                        centerLine = centerLine,
                    )

                    VisualizerStyle.RADIAL -> drawRadial(
                        bands = smoothBands,
                        sweep = sweep,
                        glow = glow,
                        innerRadiusFraction = innerRadiusFraction,
                        bass = bassSmooth[0],
                        primary = primary,
                    )

                    VisualizerStyle.OSCILLOSCOPE -> drawOscilloscope(
                        wave = smoothWave,
                        path = path,
                        primary = primary,
                        secondary = secondary,
                    )

                    VisualizerStyle.RETRO_GRID -> drawRetroGrid(
                        phase = gridPhase[0],
                        bass = bassSmooth[0],
                        primary = primary,
                        secondary = secondary,
                        sunBrush = sunBrush,
                        path = path,
                    )
                }
            }
        },
    )
}

private fun DrawScope.drawBars(
    bands: FloatArray,
    peaks: FloatArray,
    barGradient: Brush,
    glow: Color,
    peakColor: Color,
    reduceMotion: Boolean,
) {
    val w = size.width
    val h = size.height
    val n = bands.size
    val gap = w * 0.006f
    val barW = ((w - gap * (n - 1)) / n).coerceAtLeast(1f)
    val maxH = h * 0.92f
    val floor = 0.015f
    drawLine(
        color = glow,
        start = Offset(0f, h - 1.dp.toPx()),
        end = Offset(w, h - 1.dp.toPx()),
        strokeWidth = 1.dp.toPx(),
        cap = StrokeCap.Round,
    )
    for (i in 0 until n) {
        val v = bands[i].coerceIn(0f, 1f)
        if (v < floor) continue
        val bh = (v * maxH).coerceAtLeast(barW * 0.6f)
        val x = i * (barW + gap)
        val top = h - bh
        drawRoundRect(
            color = glow,
            topLeft = Offset(x - barW * 0.35f, top),
            size = Size(barW * 1.7f, bh),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW, barW),
        )
        drawRoundRect(
            brush = barGradient,
            topLeft = Offset(x, top),
            size = Size(barW, bh),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2f, barW / 2f),
        )
        if (!reduceMotion) {
            val pv = peaks[i].coerceIn(0f, 1f)
            if (pv < floor) continue
            val py = (h - (pv * maxH).coerceAtLeast(barW * 0.6f) - barW * 1.1f).coerceAtLeast(0f)
            drawRoundRect(
                color = peakColor.copy(alpha = 0.85f),
                topLeft = Offset(x, py),
                size = Size(barW, barW * 0.5f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2f, barW / 2f),
            )
        }
    }
}

private fun DrawScope.drawMirrored(
    bands: FloatArray,
    barGradient: Brush,
    glow: Color,
    centerLine: Color,
) {
    val w = size.width
    val h = size.height
    val cy = h / 2f
    val n = bands.size
    val gap = w * 0.006f
    val barW = ((w - gap * (n - 1)) / n).coerceAtLeast(1f)
    val maxH = h * 0.46f
    for (i in 0 until n) {
        val v = bands[i].coerceIn(0f, 1f)
        val bh = (v * maxH).coerceAtLeast(barW * 0.5f)
        val x = i * (barW + gap)
        drawRoundRect(
            color = glow,
            topLeft = Offset(x - barW * 0.3f, cy - bh),
            size = Size(barW * 1.6f, bh * 2f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW, barW),
        )
        drawRoundRect(
            brush = barGradient,
            topLeft = Offset(x, cy - bh),
            size = Size(barW, bh),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2f, barW / 2f),
        )
        drawRoundRect(
            brush = barGradient,
            topLeft = Offset(x, cy),
            size = Size(barW, bh),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2f, barW / 2f),
        )
    }
    drawLine(
        color = centerLine,
        start = Offset(0f, cy),
        end = Offset(w, cy),
        strokeWidth = 1.5.dp.toPx(),
    )
}

private fun DrawScope.drawRadial(
    bands: FloatArray,
    sweep: Brush,
    glow: Color,
    innerRadiusFraction: Float,
    bass: Float,
    primary: Color,
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val minDim = minOf(w, h)
    val maxRadius = minDim / 2f
    val r0 = (innerRadiusFraction * maxRadius).coerceIn(0f, maxRadius)
    val maxLen = (maxRadius - r0).coerceAtLeast(1f)
    val n = bands.size
    val half = n / 2
    val barW = (2f * PI.toFloat() * r0 / n * 0.55f).coerceAtLeast(1f)
    for (i in 0 until n) {
        val idx = if (i < half) i else n - 1 - i
        val v = bands[idx].coerceIn(0f, 1f)
        val len = (v * maxLen * 0.92f).coerceAtLeast(barW * 0.6f)
        val angle = (-90f + i * (360f / n)) * PI.toFloat() / 180f
        val dx = cos(angle)
        val dy = sin(angle)
        val start = Offset(cx + dx * r0, cy + dy * r0)
        val end = Offset(cx + dx * (r0 + len), cy + dy * (r0 + len))
        drawLine(
            color = glow,
            start = start,
            end = end,
            strokeWidth = barW * 1.8f,
            cap = StrokeCap.Round,
        )
        drawLine(
            brush = sweep,
            start = start,
            end = end,
            strokeWidth = barW,
            cap = StrokeCap.Round,
        )
    }
    drawCircle(
        color = primary.copy(alpha = (0.18f + bass * 0.35f).coerceAtMost(0.6f)),
        radius = r0,
        center = Offset(cx, cy),
        style = Stroke(width = 1.5.dp.toPx()),
    )
}

private fun DrawScope.drawOscilloscope(
    wave: FloatArray,
    path: Path,
    primary: Color,
    secondary: Color,
) {
    val w = size.width
    val h = size.height
    val cy = h / 2f
    val amp = h * 0.42f
    val n = wave.size
    if (n < 2) return
    path.rewind()
    for (i in 0 until n) {
        val x = w * i / (n - 1).toFloat()
        val y = cy - wave[i].coerceIn(-1f, 1f) * amp
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(
        path = path,
        color = secondary.copy(alpha = 0.12f),
        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
    )
    drawPath(
        path = path,
        color = primary.copy(alpha = 0.28f),
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
    )
    drawPath(
        path = path,
        color = primary,
        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawRetroGrid(
    phase: Float,
    bass: Float,
    primary: Color,
    secondary: Color,
    sunBrush: Brush,
    path: Path,
) {
    val w = size.width
    val h = size.height
    val horizon = h * 0.45f
    val bottom = h
    val baseAlpha = 0.22f + bass * 0.35f
    val rows = 12
    for (i in 0 until rows) {
        val f = ((i / rows.toFloat()) + phase) % 1f
        val y = horizon + (bottom - horizon) * f * f
        drawLine(
            color = primary.copy(alpha = (baseAlpha * (0.35f + f * 0.65f)).coerceAtMost(0.85f)),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1f,
        )
    }
    val vanishing = Offset(w / 2f, horizon)
    for (i in -8..8) {
        val x = w / 2f + i * w * 0.09f
        drawLine(
            color = primary.copy(alpha = (baseAlpha * 0.8f).coerceAtMost(0.8f)),
            start = vanishing,
            end = Offset(x, bottom),
            strokeWidth = 1f,
        )
    }
    val sunRadius = minOf(w, h) * 0.16f * (1f + bass * 0.06f)
    val sunCenter = Offset(w / 2f, horizon)
    path.rewind()
    path.addOval(Rect(center = sunCenter, radius = sunRadius))
    clipPath(path) {
        val bands = 9
        var y = horizon - sunRadius
        for (i in 0 until bands) {
            val bandHeight = sunRadius * 0.11f
            val gap = sunRadius * 0.02f * i
            drawRect(
                brush = sunBrush,
                topLeft = Offset(0f, y),
                size = Size(w, bandHeight),
            )
            y += bandHeight + gap
        }
    }
    drawLine(
        color = primary.copy(alpha = (0.55f + bass * 0.3f).coerceAtMost(0.95f)),
        start = Offset(0f, horizon),
        end = Offset(w, horizon),
        strokeWidth = 2.dp.toPx(),
    )
    drawCircle(
        color = secondary.copy(alpha = 0.10f),
        radius = sunRadius * 1.4f,
        center = sunCenter,
        style = Stroke(width = 1.dp.toPx()),
    )
}
