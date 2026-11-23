package com.neonplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.neonplayer.data.repo.LibraryRepository
import com.neonplayer.domain.model.RepeatMode
import com.neonplayer.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Immutable
data class PlayerUiState(
    val connected: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentTrack: Track? = null,
    val currentIndex: Int = -1,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val playbackSpeed: Float = 1f,
)

@Immutable
data class QueueEntry(
    val key: String,
    val index: Int,
    val track: Track,
)

class PlayerConnection(
    private val context: Context,
    private val library: LibraryRepository,
    private val scope: CoroutineScope,
) {

    private val controllerState = MutableStateFlow<MediaController?>(null)
    private var controller: MediaController? = null
    private var connecting = false
    private var tickerJob: Job? = null

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _queue = MutableStateFlow<List<QueueEntry>>(emptyList())
    val queue: StateFlow<List<QueueEntry>> = _queue.asStateFlow()

    private val _upNext = MutableStateFlow<List<QueueEntry>>(emptyList())
    val upNext: StateFlow<List<QueueEntry>> = _upNext.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refresh()
        }
    }

    fun connect() {
        if (controller != null || connecting) return
        connecting = true
        scope.launch {
            try {
                val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
                val built = MediaController.Builder(context, token).buildAsync().await()
                controller = built
                controllerState.value = built
                built.addListener(listener)
                refresh()
                startTicker()
            } catch (_: Throwable) {
                connecting = false
            }
        }
    }

    fun release() {
        tickerJob?.cancel()
        tickerJob = null
        val current = controller
        controller = null
        controllerState.value = null
        connecting = false
        if (current != null) {
            runCatching { current.removeListener(listener) }
            runCatching { current.release() }
        }
        _state.value = PlayerUiState()
        _positionMs.value = 0L
        _queue.value = emptyList()
        _upNext.value = emptyList()
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        scope.launch {
            val player = awaitController() ?: return@launch
            player.setMediaItems(tracks.map { it.toMediaItem() }, startIndex.coerceIn(0, tracks.size - 1), 0L)
            player.shuffleModeEnabled = shuffle
            player.prepare()
            player.play()
        }
    }

    fun playNext(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        scope.launch {
            val player = awaitController() ?: return@launch
            if (player.mediaItemCount == 0) {
                player.setMediaItems(tracks.map { it.toMediaItem() }, 0, 0L)
                player.prepare()
                player.play()
            } else {
                val at = (player.currentMediaItemIndex + 1).coerceIn(0, player.mediaItemCount)
                player.addMediaItems(at, tracks.map { it.toMediaItem() })
            }
        }
    }

    fun addToQueue(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        scope.launch {
            val player = awaitController() ?: return@launch
            player.addMediaItems(tracks.map { it.toMediaItem() })
        }
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) player.pause() else play()
    }

    fun play() {
        val player = controller ?: return
        if (player.playbackState == Player.STATE_IDLE) player.prepare()
        player.play()
    }

    fun pause() {
        controller?.pause()
    }

    fun stop() {
        val player = controller ?: return
        player.pause()
        player.seekTo(0L)
    }

    fun next() {
        controller?.seekToNext()
    }

    fun previous() {
        controller?.seekToPrevious()
    }

    fun seekTo(ms: Long) {
        val player = controller ?: return
        player.seekTo(ms.coerceAtLeast(0L))
        _positionMs.value = ms.coerceAtLeast(0L)
    }

    fun skipToIndex(index: Int) {
        val player = controller ?: return
        if (index !in 0 until player.mediaItemCount) return
        player.seekTo(index, 0L)
        player.play()
    }

    fun toggleShuffle() {
        val player = controller ?: return
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    fun cycleRepeat() {
        val player = controller ?: return
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val player = controller ?: return
        if (fromIndex !in 0 until player.mediaItemCount) return
        if (toIndex !in 0 until player.mediaItemCount) return
        player.moveMediaItem(fromIndex, toIndex)
    }

    fun removeQueueItem(index: Int) {
        val player = controller ?: return
        if (index !in 0 until player.mediaItemCount) return
        player.removeMediaItem(index)
    }

    fun clearUpNext() {
        val player = controller ?: return
        val indices = _upNext.value.map { it.index }.sortedDescending()
        for (index in indices) {
            if (index in 0 until player.mediaItemCount) player.removeMediaItem(index)
        }
    }

    fun clearQueue() {
        val player = controller ?: return
        player.stop()
        player.clearMediaItems()
    }

    fun setVolume(v: Float) {
        controller?.volume = v.coerceIn(0f, 1f)
    }

    private suspend fun awaitController(): MediaController? {
        controller?.let { return it }
        connect()
        return withTimeoutOrNull(CONNECT_TIMEOUT_MS) { controllerState.filterNotNull().first() }
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (isActive) {
                val player = controller
                if (player != null && player.isPlaying) {
                    _positionMs.value = player.currentPosition.coerceAtLeast(0L)
                }
                delay(POSITION_TICK_MS)
            }
        }
    }

    private fun refresh() {
        val player = controller ?: return
        val count = player.mediaItemCount
        val index = player.currentMediaItemIndex
        val current = if (index in 0 until count) player.getMediaItemAt(index).toTrackOrNull() else null
        val duration = player.duration
        _state.value = PlayerUiState(
            connected = true,
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            currentTrack = current,
            currentIndex = index,
            durationMs = if (duration > 0L) duration else current?.durationMs ?: 0L,
            shuffle = player.shuffleModeEnabled,
            repeat = player.repeatMode.toRepeatMode(),
            hasNext = player.hasNextMediaItem(),
            hasPrevious = player.hasPreviousMediaItem(),
            playbackSpeed = player.playbackParameters.speed,
        )
        _positionMs.value = player.currentPosition.coerceAtLeast(0L)
        rebuildQueue(player)
    }

    private fun rebuildQueue(player: MediaController) {
        val timeline = player.currentTimeline
        if (timeline.isEmpty || player.mediaItemCount == 0) {
            _queue.value = emptyList()
            _upNext.value = emptyList()
            return
        }
        val shuffle = player.shuffleModeEnabled
        val order = ArrayList<Int>(player.mediaItemCount)
        var windowIndex = timeline.getFirstWindowIndex(shuffle)
        var guard = 0
        while (windowIndex != C.INDEX_UNSET && guard <= player.mediaItemCount) {
            order.add(windowIndex)
            windowIndex = timeline.getNextWindowIndex(windowIndex, Player.REPEAT_MODE_OFF, shuffle)
            guard += 1
        }
        val occurrences = HashMap<String, Int>(order.size)
        val entries = ArrayList<QueueEntry>(order.size)
        for (mediaIndex in order) {
            if (mediaIndex !in 0 until player.mediaItemCount) continue
            val item = player.getMediaItemAt(mediaIndex)
            val seen = occurrences[item.mediaId] ?: 0
            occurrences[item.mediaId] = seen + 1
            val track = item.toTrackOrNull() ?: continue
            entries.add(QueueEntry(key = "${item.mediaId}#$seen", index = mediaIndex, track = track))
        }
        _queue.value = entries
        val currentIndex = player.currentMediaItemIndex
        val position = entries.indexOfFirst { it.index == currentIndex }
        _upNext.value = if (position >= 0) entries.drop(position + 1) else emptyList()
    }

    private fun Int.toRepeatMode(): RepeatMode = when (this) {
        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
        else -> RepeatMode.OFF
    }

    private companion object {
        const val POSITION_TICK_MS = 250L
        const val CONNECT_TIMEOUT_MS = 5_000L
    }
}
