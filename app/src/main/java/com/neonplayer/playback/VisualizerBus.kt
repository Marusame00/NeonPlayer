package com.neonplayer.playback

object VisualizerBus {

    const val BANDS = 64
    const val WAVE_SIZE = 256

    @Volatile
    var frameId: Long = 0L
        private set

    @Volatile
    var sensitivity: Float = 1f

    private val lock = Any()
    private val bandsFront = FloatArray(BANDS)
    private val bandsBack = FloatArray(BANDS)
    private val waveFront = FloatArray(WAVE_SIZE)
    private val waveBack = FloatArray(WAVE_SIZE)

    private var bandsWrite = bandsFront
    private var bandsRead = bandsBack
    private var waveWrite = waveFront
    private var waveRead = waveBack

    @Volatile
    private var bassValue = 0f

    @Volatile
    private var levelValue = 0f

    val bass: Float get() = bassValue

    val level: Float get() = levelValue

    fun readBands(dest: FloatArray) {
        synchronized(lock) {
            val count = minOf(dest.size, BANDS)
            System.arraycopy(bandsRead, 0, dest, 0, count)
            if (dest.size > count) dest.fill(0f, count, dest.size)
        }
    }

    fun readWave(dest: FloatArray) {
        synchronized(lock) {
            val count = minOf(dest.size, WAVE_SIZE)
            System.arraycopy(waveRead, 0, dest, 0, count)
            if (dest.size > count) dest.fill(0f, count, dest.size)
        }
    }

    internal fun publish(bands: FloatArray, wave: FloatArray, bass: Float, level: Float) {
        synchronized(lock) {
            System.arraycopy(bands, 0, bandsWrite, 0, BANDS)
            System.arraycopy(wave, 0, waveWrite, 0, WAVE_SIZE)
            val swapBands = bandsWrite
            bandsWrite = bandsRead
            bandsRead = swapBands
            val swapWave = waveWrite
            waveWrite = waveRead
            waveRead = swapWave
        }
        bassValue = bass
        levelValue = level
        frameId += 1L
    }

    internal fun clear() {
        synchronized(lock) {
            bandsRead.fill(0f)
            bandsWrite.fill(0f)
            waveRead.fill(0f)
            waveWrite.fill(0f)
        }
        bassValue = 0f
        levelValue = 0f
        frameId += 1L
    }
}
