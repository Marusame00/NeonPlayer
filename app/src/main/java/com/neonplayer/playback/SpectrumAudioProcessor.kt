package com.neonplayer.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

class SpectrumAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var replayGainLinear: Float = 1f

    private val ring = FloatArray(RING_SIZE)
    private val ringLock = Any()
    private var ringWrite = 0

    @Volatile
    private var sampleCount = 0L

    @Volatile
    private var lastInputNanos = 0L

    @Volatile
    private var running = false

    private var thread: Thread? = null

    private val scratch = FloatArray(SCRATCH_SIZE)
    private val window = FloatArray(RING_SIZE)
    private val hann = FloatArray(RING_SIZE)
    private val re = FloatArray(RING_SIZE)
    private val im = FloatArray(RING_SIZE)
    private val magnitudes = FloatArray(RING_SIZE / 2)
    private val bands = FloatArray(VisualizerBus.BANDS)
    private val smoothed = FloatArray(VisualizerBus.BANDS)
    private val wave = FloatArray(VisualizerBus.WAVE_SIZE)
    private val fft = Fft(RING_SIZE)
    private val bandStart = IntArray(VisualizerBus.BANDS)
    private val bandEnd = IntArray(VisualizerBus.BANDS)

    @Volatile
    private var bandSampleRate = -1

    init {
        for (i in 0 until RING_SIZE) {
            hann[i] = (0.5f - 0.5f * kotlin.math.cos(2.0 * Math.PI * i / RING_SIZE).toFloat())
        }
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        val encoding = inputAudioFormat.encoding
        if (encoding != C.ENCODING_PCM_16BIT && encoding != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        ensureBands(inputAudioFormat.sampleRate)
        startAnalysis()
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining <= 0) return
        val output = replaceOutputBuffer(remaining)
        val gain = replayGainLinear
        val encoding = inputAudioFormat.encoding
        val channels = inputAudioFormat.channelCount.coerceAtLeast(1)
        val bytesPerSample = if (encoding == C.ENCODING_PCM_FLOAT) 4 else 2
        val frameBytes = bytesPerSample * channels
        val frames = remaining / frameBytes
        val perChunk = (scratch.size / channels).coerceAtLeast(1)
        var done = 0
        while (done < frames) {
            val chunk = minOf(frames - done, perChunk)
            val count = chunk * channels
            if (encoding == C.ENCODING_PCM_FLOAT) {
                for (i in 0 until count) scratch[i] = inputBuffer.getFloat()
                if (gain == 1f) {
                    for (i in 0 until count) {
                        val v = scratch[i]
                        output.putFloat(if (v > 1f) 1f else if (v < -1f) -1f else v)
                    }
                } else {
                    for (i in 0 until count) {
                        val v = scratch[i] * gain
                        output.putFloat(if (v > 1f) 1f else if (v < -1f) -1f else v)
                    }
                }
            } else {
                for (i in 0 until count) scratch[i] = inputBuffer.getShort().toFloat() / 32768f
                if (gain == 1f) {
                    for (i in 0 until count) {
                        output.putShort((scratch[i] * 32768f).toInt().coerceIn(-32768, 32767).toShort())
                    }
                } else {
                    for (i in 0 until count) {
                        output.putShort((scratch[i] * 32768f * gain).toInt().coerceIn(-32768, 32767).toShort())
                    }
                }
            }
            pushMono(chunk, channels)
            done += chunk
        }
        while (inputBuffer.hasRemaining()) output.put(inputBuffer.get())
        output.flip()
    }

    override fun onFlush() {
        synchronized(ringLock) {
            ring.fill(0f)
            ringWrite = 0
            sampleCount = 0L
        }
        smoothed.fill(0f)
        bands.fill(0f)
        wave.fill(0f)
        VisualizerBus.clear()
    }

    override fun onReset() {
        stopAnalysis()
        onFlush()
    }

    fun release() {
        stopAnalysis()
    }

    private fun pushMono(frames: Int, channels: Int) {
        synchronized(ringLock) {
            var write = ringWrite
            for (frame in 0 until frames) {
                var sum = 0f
                val base = frame * channels
                for (channel in 0 until channels) sum += scratch[base + channel]
                ring[write] = sum / channels
                write = (write + 1) and RING_MASK
            }
            ringWrite = write
            sampleCount += frames
            lastInputNanos = System.nanoTime()
        }
    }

    private fun startAnalysis() {
        if (running) return
        running = true
        val worker = Thread({ analysisLoop() }, "NeonSpectrum")
        worker.isDaemon = true
        thread = worker
        worker.start()
    }

    private fun stopAnalysis() {
        running = false
        val worker = thread
        thread = null
        if (worker != null) runCatching { worker.join(250) }
    }

    private fun analysisLoop() {
        var lastCount = -1L
        while (running) {
            val count = sampleCount
            if (count != lastCount) {
                lastCount = count
                analyze()
                sleep(16L)
            } else if ((System.nanoTime() - lastInputNanos) / 1_000_000L > IDLE_MS) {
                decay()
                sleep(50L)
            } else {
                sleep(8L)
            }
        }
    }

    private fun analyze() {
        synchronized(ringLock) {
            var read = ringWrite
            for (i in 0 until RING_SIZE) {
                window[i] = ring[read]
                read = (read + 1) and RING_MASK
            }
        }
        var sumSquares = 0f
        for (i in 0 until RING_SIZE) {
            val sample = window[i]
            sumSquares += sample * sample
            re[i] = sample * hann[i]
            im[i] = 0f
        }
        val level = sqrt(sumSquares / RING_SIZE).coerceIn(0f, 1f)
        fft.transform(re, im)
        for (i in 0 until RING_SIZE / 2) {
            val real = re[i]
            val imag = im[i]
            magnitudes[i] = sqrt(real * real + imag * imag) * MAG_NORM
        }
        val sensitivity = VisualizerBus.sensitivity
        for (band in 0 until VisualizerBus.BANDS) {
            var peak = 0f
            for (bin in bandStart[band] until bandEnd[band]) {
                if (magnitudes[bin] > peak) peak = magnitudes[bin]
            }
            val db = 20f * log10(peak + 1e-7f) + band * TILT_DB_PER_BAND
            val target = (((db + FLOOR_DB) / FLOOR_DB).coerceIn(0f, 1f) * sensitivity).coerceIn(0f, 1f)
            val current = smoothed[band]
            smoothed[band] = if (target > current) {
                current + ATTACK * (target - current)
            } else {
                current + DECAY * (target - current)
            }
            bands[band] = smoothed[band]
        }
        var bassSum = 0f
        for (band in 0 until BASS_BANDS) bassSum += bands[band]
        val bass = (bassSum / BASS_BANDS).coerceIn(0f, 1f)
        val step = RING_SIZE / VisualizerBus.WAVE_SIZE
        for (i in 0 until VisualizerBus.WAVE_SIZE) {
            wave[i] = window[i * step].coerceIn(-1f, 1f)
        }
        VisualizerBus.publish(bands, wave, bass, level)
    }

    private fun decay() {
        var changed = false
        for (band in 0 until VisualizerBus.BANDS) {
            val value = smoothed[band] * DECAY_IDLE
            smoothed[band] = value
            bands[band] = value
            if (value > 0.001f) changed = true
        }
        for (i in 0 until VisualizerBus.WAVE_SIZE) wave[i] *= DECAY_IDLE
        if (changed) VisualizerBus.publish(bands, wave, 0f, 0f) else VisualizerBus.clear()
    }

    private fun ensureBands(sampleRate: Int) {
        if (bandSampleRate == sampleRate) return
        bandSampleRate = sampleRate
        val binHz = sampleRate.toFloat() / RING_SIZE
        val ratio = F_MAX_HZ / F_MIN_HZ
        val limit = RING_SIZE / 2
        for (band in 0 until VisualizerBus.BANDS) {
            val low = F_MIN_HZ * ratio.pow(band.toFloat() / VisualizerBus.BANDS)
            val high = F_MIN_HZ * ratio.pow((band + 1).toFloat() / VisualizerBus.BANDS)
            val start = (low / binHz).toInt().coerceIn(1, limit - 1)
            val end = (high / binHz).toInt().coerceIn(start + 1, limit)
            bandStart[band] = start
            bandEnd[band] = end
        }
    }

    private fun sleep(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            running = false
        }
    }

    private companion object {
        const val RING_SIZE = 2048
        const val RING_MASK = RING_SIZE - 1
        const val SCRATCH_SIZE = 4096
        const val BASS_BANDS = 8
        const val ATTACK = 0.6f
        const val DECAY = 0.12f
        const val DECAY_IDLE = 0.85f
        const val IDLE_MS = 100L
        const val F_MIN_HZ = 40f
        const val F_MAX_HZ = 16000f
        const val MAG_NORM = 4f / RING_SIZE
        const val TILT_DB_PER_BAND = 0.22f
        const val FLOOR_DB = 54f
    }
}
