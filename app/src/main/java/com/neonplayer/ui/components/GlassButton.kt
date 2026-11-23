package com.neonplayer.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neonplayer.ui.theme.Neon

@Composable
fun GlassButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    active: Boolean = false,
    enabled: Boolean = true,
    tint: Color = Neon.palette.primary,
) {
    val palette = Neon.palette
    val reduceMotion = Neon.style.reduceMotion
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.92f else 1f,
        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow),
        label = "glassScale",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = when {
            !enabled -> 0.12f
            pressed -> 0.60f
            active -> 0.45f
            else -> 0.26f
        },
        animationSpec = if (reduceMotion) snap() else tween(180),
        label = "glassGlow",
    )
    val circle = size >= 64.dp
    val corner = if (circle) size / 2 else size * 0.32f
    val shape: Shape = if (circle) CircleShape else RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(if (enabled) 1f else 0.4f)
            .neonGlow(
                color = tint,
                radius = if (circle) size * 0.35f else size * 0.30f,
                cornerRadius = corner,
                alpha = glowAlpha,
            )
            .clip(shape)
            .drawWithCache {
                val w = this.size.width
                val h = this.size.height
                val cr = if (circle) h / 2f else corner.toPx()
                val fill = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        tint.copy(alpha = 0.10f),
                        palette.base1.copy(alpha = 0.35f),
                    ),
                )
                val wash = Brush.radialGradient(
                    colors = listOf(tint.copy(alpha = if (active) 0.26f else 0.14f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.32f),
                    radius = w * 0.78f,
                )
                val innerShadow = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                    startY = h * 0.55f,
                    endY = h,
                )
                val specular = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.38f), Color.Transparent),
                    startY = 0f,
                    endY = h * 0.45f,
                )
                val rim = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.35f), tint.copy(alpha = 0.25f)),
                )
                val strokeWidth = 1.dp.toPx()
                onDrawBehind {
                    drawRoundRect(brush = fill, cornerRadius = CornerRadius(cr, cr))
                    drawRoundRect(brush = wash, cornerRadius = CornerRadius(cr, cr))
                    drawRoundRect(brush = innerShadow, cornerRadius = CornerRadius(cr, cr))
                    val inset = w * 0.08f
                    drawRoundRect(
                        brush = specular,
                        topLeft = Offset(inset, inset),
                        size = Size(w - inset * 2f, (h * 0.45f - inset).coerceAtLeast(0f)),
                        cornerRadius = CornerRadius(cr * 0.8f, cr * 0.8f),
                    )
                    drawRoundRect(
                        brush = rim,
                        cornerRadius = CornerRadius(cr, cr),
                        style = Stroke(width = strokeWidth),
                    )
                }
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) tint else palette.textPrimary.copy(alpha = 0.9f),
            modifier = Modifier.size(size * 0.46f),
        )
    }
}

@Composable
fun FlatIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    active: Boolean = false,
    enabled: Boolean = true,
    tint: Color = Neon.palette.primary,
) {
    val palette = Neon.palette
    val reduceMotion = Neon.style.reduceMotion
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "flatScale",
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.7f else 1f,
        animationSpec = if (reduceMotion) snap() else tween(140),
        label = "flatAlpha",
    )
    val shape = RoundedCornerShape(size * 0.32f)
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .then(
                if (active) {
                    Modifier
                        .background(tint.copy(alpha = 0.12f), shape)
                        .border(1.dp, tint.copy(alpha = 0.35f), shape)
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (active) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint.copy(alpha = 0.30f),
                modifier = Modifier.size(size * 0.58f),
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = (if (active) tint else palette.textPrimary.copy(alpha = 0.9f)).copy(alpha = iconAlpha),
            modifier = Modifier.size(size * 0.46f),
        )
    }
}

@Composable
fun NeonIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    active: Boolean = false,
    enabled: Boolean = true,
    tint: Color = Neon.palette.primary,
) {
    if (Neon.style.glassIcons) {
        GlassButton(icon, contentDescription, onClick, modifier, size, active, enabled, tint)
    } else {
        FlatIconButton(icon, contentDescription, onClick, modifier, size, active, enabled, tint)
    }
}

@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
) {
    val palette = Neon.palette
    val glass = Neon.style.glassIcons
    val reduceMotion = Neon.style.reduceMotion
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = if (reduceMotion) snap() else spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "playScale",
    )
    val glowAlpha = if (isPlaying && !reduceMotion) {
        val transition = rememberInfiniteTransition(label = "playPulse")
        val value by transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.45f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "playPulseAlpha",
        )
        value
    } else {
        0.32f
    }
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .neonGlow(palette.primary, radius = size * 0.40f, cornerRadius = size / 2, alpha = glowAlpha)
            .clip(CircleShape)
            .drawWithCache {
                val w = this.size.width
                val h = this.size.height
                val cr = h / 2f
                val orb = if (glass) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.20f),
                            palette.primary.copy(alpha = 0.22f),
                            palette.base1.copy(alpha = 0.55f),
                        ),
                    )
                } else {
                    Brush.radialGradient(
                        colors = listOf(
                            palette.primary,
                            palette.primary.copy(alpha = 0.92f),
                            palette.primary.copy(alpha = 0.55f),
                        ),
                        center = Offset(w * 0.38f, h * 0.30f),
                        radius = w * 0.85f,
                    )
                }
                val specular = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = if (glass) 0.34f else 0.22f), Color.Transparent),
                    startY = 0f,
                    endY = h * 0.48f,
                )
                val rim = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.40f), palette.primary.copy(alpha = 0.30f)),
                )
                val strokeWidth = 1.dp.toPx()
                onDrawBehind {
                    drawCircle(brush = orb, radius = w / 2f, center = Offset(w / 2f, h / 2f))
                    val inset = w * 0.09f
                    drawOval(
                        brush = specular,
                        topLeft = Offset(inset, inset),
                        size = Size(w - inset * 2f, (h * 0.48f - inset).coerceAtLeast(0f)),
                    )
                    drawCircle(
                        brush = rim,
                        radius = w / 2f - strokeWidth / 2f,
                        center = Offset(w / 2f, h / 2f),
                        style = Stroke(width = strokeWidth),
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = if (glass) 0.10f else 0.16f),
                        radius = cr * 0.62f,
                        center = Offset(w * 0.5f, h * 0.30f),
                    )
                }
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = isPlaying,
            animationSpec = if (reduceMotion) snap() else tween(220),
            label = "playIcon",
        ) { playing ->
            Icon(
                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (playing) "Pause" else "Play",
                tint = if (glass) palette.textPrimary else palette.base0,
                modifier = Modifier.size(size * 0.42f),
            )
        }
    }
}
