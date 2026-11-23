package com.neonplayer.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.neonplayer.AppContainer
import com.neonplayer.NeonApp
import com.neonplayer.domain.model.ReplayGainMode
import com.neonplayer.domain.model.RepeatMode
import com.neonplayer.domain.model.SavedPlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.math.pow

class PlaybackService : MediaSessionService() {

    private lateinit var container: AppContainer
    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null
    private val spectrumProcessor = SpectrumAudioProcessor()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var settingsVolume = 1f
    private var crossfadeMs = 0
    private var fade = 1f
    private var restoring = false
    private var queueSaveJob: Job? = null
    private var tickerJob: Job? = null
    private var tickCount = 0

    override fun onCreate() {
        super.onCreate()
        container = (application as NeonApp).container
        player = buildPlayer()
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity())
            .setCallback(SessionCallback())
            .build()
        player.addListener(PlayerListener())
        observeSettings()
        restoreQueue()
        startTicker()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        savePlaybackStateBlocking()
        if (!player.isPlaying || player.mediaItemCount == 0) stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        savePlaybackStateBlocking()
        saveQueueBlocking()
        tickerJob?.cancel()
        queueSaveJob?.cancel()
        container.equalizerManager.release()
        spectrumProcessor.release()
        runCatching { mediaSession?.release() }
        mediaSession = null
        runCatching { player.release() }
        scope.cancel()
        super.onDestroy()
    }

    private fun buildPlayer(): ExoPlayer {
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setAudioProcessors(arrayOf(spectrumProcessor))
                .setEnableFloatOutput(false)
                .build()
        }
        val attributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        return ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(attributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
            .apply { setSkipSilenceEnabled(false) }
    }

    private fun sessionActivity(): PendingIntent {
        val intent = Intent().apply {
            setClassName(packageName, MAIN_ACTIVITY)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun observeSettings() {
        scope.launch {
            container.settingsRepository.settings.collect { settings ->
                settingsVolume = settings.volume
                crossfadeMs = settings.crossfadeMs
                if (crossfadeMs <= 0) fade = 1f
                applyVolume()
            }
        }
    }

    private fun restoreQueue() {
        scope.launch {
            restoring = true
            try {
                val ids = container.queueRepository.load()
                val saved = container.settingsRepository.loadPlaybackState()
                val tracks = container.libraryRepository.getTracks(ids)
                if (tracks.isNotEmpty()) {
                    player.setMediaItems(
                        tracks.map { it.toMediaItem() },
                        saved.index.coerceIn(0, tracks.size - 1),
                        saved.positionMs,
                    )
                    player.shuffleModeEnabled = saved.shuffle
                    player.repeatMode = saved.repeat.toPlayerRepeatMode()
                    player.prepare()
                }
                player.volume = container.settingsRepository.settings.value.volume
                container.equalizerManager.attach(player.audioSessionId)
            } finally {
                restoring = false
            }
        }
    }

    private fun applyVolume() {
        player.volume = (settingsVolume * fade).coerceIn(0f, 1f)
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (isActive) {
                tick()
                delay(TICK_MS)
            }
        }
    }

    private fun tick() {
        if (!player.isPlaying) return
        if (crossfadeMs > 0) {
            val duration = player.duration
            val position = player.currentPosition
            val fadeMs = crossfadeMs.toLong()
            val target = when {
                duration > 0L && position >= duration - fadeMs ->
                    ((duration - position).coerceAtLeast(0L).toFloat() / fadeMs).coerceIn(0f, 1f)

                position < fadeMs -> (position.toFloat() / fadeMs).coerceIn(0f, 1f)
                else -> 1f
            }
            if (target != fade) {
                fade = target
                applyVolume()
            }
        }
        tickCount += 1
        if (tickCount >= STATE_SAVE_TICKS) {
            tickCount = 0
            savePlaybackState()
        }
    }

    private fun scheduleQueueSave() {
        if (restoring) return
        queueSaveJob?.cancel()
        queueSaveJob = scope.launch {
            delay(QUEUE_SAVE_DEBOUNCE_MS)
            val ids = (0 until player.mediaItemCount).mapNotNull { player.getMediaItemAt(it).mediaId.toLongOrNull() }
            container.queueRepository.save(ids)
        }
    }

    private fun saveQueueBlocking() {
        val ids = (0 until player.mediaItemCount).mapNotNull { player.getMediaItemAt(it).mediaId.toLongOrNull() }
        runBlocking { withContext(Dispatchers.IO) { container.queueRepository.save(ids) } }
    }

    private fun currentState(): SavedPlaybackState = SavedPlaybackState(
        index = player.currentMediaItemIndex.coerceAtLeast(0),
        positionMs = player.currentPosition.coerceAtLeast(0L),
        shuffle = player.shuffleModeEnabled,
        repeat = player.repeatMode.toRepeatMode(),
    )

    private fun savePlaybackState() {
        if (restoring) return
        val state = currentState()
        scope.launch { container.settingsRepository.savePlaybackState(state) }
    }

    private fun savePlaybackStateBlocking() {
        val state = currentState()
        runBlocking { withContext(Dispatchers.IO) { container.settingsRepository.savePlaybackState(state) } }
    }

    private fun handleError(error: PlaybackException) {
        val item = player.currentMediaItem
        val title = item?.mediaMetadata?.title?.toString().orEmpty()
        val message = if (title.isEmpty()) error.errorCodeName else "${error.errorCodeName}: $title"
        container.playbackEvents.tryEmit(message)
        if (error.errorCode !in FATAL_ERROR_CODES) return
        val trackId = item?.mediaId?.toLongOrNull() ?: return
        val index = player.currentMediaItemIndex
        scope.launch {
            container.libraryRepository.markMissing(trackId)
            if (player.mediaItemCount <= 1) {
                player.stop()
                return@launch
            }
            player.removeMediaItem(index)
            val target = index.coerceAtMost(player.mediaItemCount - 1)
            player.seekTo(target, 0L)
            player.prepare()
            player.play()
        }
    }

    private fun applyReplayGain(tracks: Tracks) {
        val mode = container.settingsRepository.settings.value.replayGain
        if (mode == ReplayGainMode.OFF) {
            spectrumProcessor.replayGainLinear = 1f
            return
        }
        var gainDb: Float? = null
        for (group in tracks.groups) {
            for (index in 0 until group.length) {
                val format = group.getTrackFormat(index) ?: continue
                val metadata = format.metadata ?: continue
                gainDb = readGain(metadata, mode)
                if (gainDb != null) break
            }
            if (gainDb != null) break
        }
        val db = gainDb
        spectrumProcessor.replayGainLinear = if (db == null) {
            1f
        } else {
            10f.pow(db / 20f).coerceIn(0.1f, 2f)
        }
    }

    private fun readGain(metadata: Metadata, mode: ReplayGainMode): Float? {
        var track: Float? = null
        var album: Float? = null
        for (index in 0 until metadata.length()) {
            when (val entry = metadata.get(index)) {
                is VorbisComment -> {
                    val key = entry.key.uppercase()
                    if (key == KEY_TRACK_GAIN) track = parseGainDb(entry.value)
                    if (key == KEY_ALBUM_GAIN) album = parseGainDb(entry.value)
                }

                is TextInformationFrame -> {
                    val key = entry.description?.uppercase()
                    if (key == KEY_TRACK_GAIN) track = parseGainDb(entry.value)
                    if (key == KEY_ALBUM_GAIN) album = parseGainDb(entry.value)
                }
            }
        }
        return if (mode == ReplayGainMode.ALBUM) album ?: track else track
    }

    private fun parseGainDb(raw: String?): Float? {
        if (raw.isNullOrBlank()) return null
        val match = GAIN_REGEX.find(raw) ?: return null
        return match.value.toFloatOrNull()
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> = Futures.immediateFuture(mediaItems.map { it.ensurePlayableUri() })

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startTimeMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val index = if (startIndex == C.INDEX_UNSET) 0 else startIndex
            return Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(mediaItems.map { it.ensurePlayableUri() }, index, startTimeMs),
            )
        }
    }

    private inner class PlayerListener : Player.Listener {
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            scheduleQueueSave()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (crossfadeMs > 0) {
                fade = 0f
                applyVolume()
            }
            savePlaybackState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            savePlaybackState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            savePlaybackState()
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            container.equalizerManager.attach(audioSessionId)
        }

        override fun onTracksChanged(tracks: Tracks) {
            applyReplayGain(tracks)
        }

        override fun onPlayerError(error: PlaybackException) {
            handleError(error)
        }
    }

    private fun Int.toRepeatMode(): RepeatMode = when (this) {
        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
        else -> RepeatMode.OFF
    }

    private fun RepeatMode.toPlayerRepeatMode(): Int = when (this) {
        RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        RepeatMode.OFF -> Player.REPEAT_MODE_OFF
    }

    private companion object {
        const val MAIN_ACTIVITY = "com.neonplayer.MainActivity"
        const val TICK_MS = 50L
        const val STATE_SAVE_TICKS = 100
        const val QUEUE_SAVE_DEBOUNCE_MS = 500L
        const val KEY_TRACK_GAIN = "REPLAYGAIN_TRACK_GAIN"
        const val KEY_ALBUM_GAIN = "REPLAYGAIN_ALBUM_GAIN"
        val GAIN_REGEX = Regex("[-+]?\\d+(\\.\\d+)?")
        val FATAL_ERROR_CODES = setOf(
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        )
    }
}
