package com.neonplayer.playback

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class Fft(private val size: Int) {

    private val cosTable = FloatArray(size / 2)
    private val sinTable = FloatArray(size / 2)
    private val reverse = IntArray(size)

    init {
        for (i in 0 until size / 2) {
            cosTable[i] = cos(2.0 * PI * i / size).toFloat()
            sinTable[i] = sin(2.0 * PI * i / size).toFloat()
        }
        val bits = Integer.numberOfTrailingZeros(size)
        for (i in 0 until size) reverse[i] = Integer.reverse(i) ushr (32 - bits)
    }

    fun transform(re: FloatArray, im: FloatArray) {
        for (i in 0 until size) {
            val j = reverse[i]
            if (j > i) {
                val tr = re[i]
                re[i] = re[j]
                re[j] = tr
                val ti = im[i]
                im[i] = im[j]
                im[j] = ti
            }
        }
        var span = 2
        while (span <= size) {
            val half = span / 2
            val step = size / span
            var start = 0
            while (start < size) {
                var k = 0
                for (i in start until start + half) {
                    val j = i + half
                    val wr = cosTable[k]
                    val wi = -sinTable[k]
                    val xr = re[j] * wr - im[j] * wi
                    val xi = re[j] * wi + im[j] * wr
                    re[j] = re[i] - xr
                    im[j] = im[i] - xi
                    re[i] += xr
                    im[i] += xi
                    k += step
                }
                start += span
            }
            span *= 2
        }
    }
}
