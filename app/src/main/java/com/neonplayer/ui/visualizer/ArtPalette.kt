package com.neonplayer.ui.visualizer

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.neonplayer.data.art.ArtModel
import com.neonplayer.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min

private const val HUE_BUCKETS = 36
private const val SAMPLE_SIZE = 48

@Composable
fun rememberArtAccents(track: Track?, enabled: Boolean): Pair<Color, Color>? {
    val context = LocalContext.current
    var accents by remember(track?.uri, enabled) { mutableStateOf<Pair<Color, Color>?>(null) }
    LaunchedEffect(track?.uri, enabled) {
        val uri = track?.uri
        if (!enabled || uri == null) {
            accents = null
            return@LaunchedEffect
        }
        accents = withContext(Dispatchers.Default) { computeAccents(context, uri) }
    }
    return accents
}

private suspend fun computeAccents(context: Context, trackUri: String): Pair<Color, Color>? {
    val request = ImageRequest.Builder(context)
        .data(ArtModel(trackUri))
        .size(SAMPLE_SIZE)
        .allowHardware(false)
        .build()
    val result = runCatching { SingletonImageLoader.get(context).execute(request) }.getOrNull() as? SuccessResult
        ?: return null
    val bitmap = runCatching { result.image.toBitmap() }.getOrNull() ?: return null
    if (bitmap.width <= 0 || bitmap.height <= 0) return null
    val weight = FloatArray(HUE_BUCKETS)
    val satSum = FloatArray(HUE_BUCKETS)
    val valSum = FloatArray(HUE_BUCKETS)
    val hsv = FloatArray(3)
    for (y in 0 until bitmap.height) {
        for (x in 0 until bitmap.width) {
            val pixel = bitmap.getPixel(x, y)
            if (AndroidColor.alpha(pixel) < 32) continue
            AndroidColor.colorToHSV(pixel, hsv)
            val s = hsv[1]
            val v = hsv[2]
            if (s < 0.12f || v < 0.10f) continue
            val bucket = ((hsv[0] / 360f) * HUE_BUCKETS).toInt().coerceIn(0, HUE_BUCKETS - 1)
            val w = s * v
            weight[bucket] += w
            satSum[bucket] += s * w
            valSum[bucket] += v * w
        }
    }
    val first = weight.indices.maxByOrNull { weight[it] } ?: return null
    if (weight[first] <= 0f) return null
    val second = weight.indices
        .filter { hueDistance(it, first) >= 90f }
        .maxByOrNull { weight[it] }
        ?: ((first + HUE_BUCKETS / 3) % HUE_BUCKETS)
    fun colorFor(bucket: Int): Color {
        val w = weight[bucket]
        val s = if (w > 0f) (satSum[bucket] / w).coerceIn(0.6f, 1f) else 0.7f
        val v = if (w > 0f) (valSum[bucket] / w).coerceIn(0.85f, 1f) else 0.9f
        val hue = (bucket + 0.5f) / HUE_BUCKETS * 360f
        return Color(AndroidColor.HSVToColor(floatArrayOf(hue, s, v)))
    }
    return colorFor(first) to colorFor(second)
}

private fun hueDistance(a: Int, b: Int): Float {
    val d = abs(a - b)
    return min(d, HUE_BUCKETS - d) * (360f / HUE_BUCKETS)
}
