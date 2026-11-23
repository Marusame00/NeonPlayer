package com.neonplayer.ui.stage

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

fun Modifier.burnInDrift(enabled: Boolean): Modifier = composed {
    if (!enabled) return@composed this
    val density = LocalDensity.current
    val ampX = with(density) { 10.dp.toPx() }
    val ampY = with(density) { 6.dp.toPx() }
    val transition = rememberInfiniteTransition(label = "burnInDrift")
    val xPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 240_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "driftX",
    )
    val yPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 170_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "driftY",
    )
    graphicsLayer {
        translationX = sin(xPhase * 2f * PI.toFloat()) * ampX
        translationY = sin(yPhase * 2f * PI.toFloat()) * ampY
    }
}

@Composable
fun DimOverlay(
    dimAfterMinutes: Int,
    lastInteractionMs: Long,
    modifier: Modifier = Modifier,
) {
    var dimmed by remember { mutableStateOf(false) }
    LaunchedEffect(dimAfterMinutes, lastInteractionMs) {
        dimmed = false
        if (dimAfterMinutes <= 0) return@LaunchedEffect
        delay(dimAfterMinutes * 60_000L)
        dimmed = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (dimmed) 0.55f else 0f,
        animationSpec = tween(durationMillis = 3000),
        label = "dimAlpha",
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .background(Color.Black),
    )
}
