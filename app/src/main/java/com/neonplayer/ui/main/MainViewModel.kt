package com.neonplayer.ui.main

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neonplayer.NeonApp
import com.neonplayer.domain.model.AlbumGroup
import com.neonplayer.domain.model.AppSettings
import com.neonplayer.domain.model.ArtistGroup
import com.neonplayer.domain.model.BACKDROP_CUSTOM_PREFIX
import com.neonplayer.domain.model.BACKDROP_STATIC
import com.neonplayer.domain.model.FolderGroup
import com.neonplayer.domain.model.LibraryTab
import com.neonplayer.domain.model.Playlist
import com.neonplayer.domain.model.ScanState
import com.neonplayer.domain.model.SortOrder
import com.neonplayer.domain.model.Track
import com.neonplayer.playback.EqUiState
import com.neonplayer.playback.PlayerUiState
import com.neonplayer.playback.QueueEntry
import com.neonplayer.playback.SleepTimerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Overlay { NONE, SETTINGS, EQUALIZER, BACKDROPS }

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as NeonApp).container
    private val settingsRepository = container.settingsRepository
    private val libraryRepository = container.libraryRepository
    private val playlistRepository = container.playlistRepository
    private val connection = container.playerConnection
    private val equalizer = container.equalizerManager
    private val sleep = container.sleepTimer
    private val scope = viewModelScope

    val settings: StateFlow<AppSettings> = settingsRepository.settings
    val player: StateFlow<PlayerUiState> = connection.state
    val positionMs: StateFlow<Long> = connection.positionMs
    val queue: StateFlow<List<QueueEntry>> = connection.queue
    val upNext: StateFlow<List<QueueEntry>> = connection.upNext
    val scanState: StateFlow<ScanState> = libraryRepository.scanState
    val eqState: StateFlow<EqUiState> = equalizer.state
    val sleepTimer: StateFlow<SleepTimerState> = sleep.state

    val trackCount: StateFlow<Int> = libraryRepository.trackCount()
        .stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_MS), 0)

    val playlists: StateFlow<List<Playlist>> = playlistRepository.playlists()
        .stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_MS), emptyList())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = MESSAGE_BUFFER)

    val events: SharedFlow<String> = merge(container.playbackEvents, _messages)
        .shareIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_MS), replay = 0)

    val query = MutableStateFlow("")

    private val debouncedQuery: StateFlow<String> = query
        .debounce(QUERY_DEBOUNCE_MS)
        .map { it.trim() }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_MS), "")

    val tab: StateFlow<LibraryTab> = settings
        .map { it.libraryTab }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_MS), LibraryTab.SONGS)

    val sort: StateFlow<SortOrder> = settings
        .map { it.librarySort }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_MS), SortOrder.TITLE)

    val songs: StateFlow<List<Track>> = combine(sort, debouncedQuery) { order, text -> order to text }
        .flatMapLatest { (order, text) -> libraryRepository.tracks(order, text) }
        .stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_MS), emptyList())

    val albums: StateFlow<List<AlbumGroup>> = debouncedQuery
        .flatMapLatest { libraryRepository.albums(it) }
        .stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_MS), emptyList())

    val artists: StateFlow<List<ArtistGroup>> = debouncedQuery
        .flatMapLatest { libraryRepository.artists(it) }
        .stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_MS), emptyList())

    val folders: StateFlow<List<FolderGroup>> = debouncedQuery
        .flatMapLatest { libraryRepository.folders(it) }
        .stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_MS), emptyList())

    private val _openPlaylistId = MutableStateFlow<Long?>(null)
    val openPlaylistId: StateFlow<Long?> = _openPlaylistId.asStateFlow()

    val openPlaylistTracks: StateFlow<List<Track>> = _openPlaylistId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else playlistRepository.playlistTracks(id) }
        .stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_MS), emptyList())

    private val _stageMode = MutableStateFlow(false)
    val stageMode: StateFlow<Boolean> = _stageMode.asStateFlow()

    private val _overlay = MutableStateFlow(Overlay.NONE)
    val overlay: StateFlow<Overlay> = _overlay.asStateFlow()

    private val _lastInteractionMs = MutableStateFlow(System.currentTimeMillis())
    val lastInteractionMs: StateFlow<Long> = _lastInteractionMs.asStateFlow()

    private var volumeJob: Job? = null

    fun onUserInteraction() {
        _lastInteractionMs.value = System.currentTimeMillis()
    }

    fun enterStage() {
        _stageMode.value = true
    }

    fun exitStage() {
        _stageMode.value = false
    }

    fun openOverlay(target: Overlay) {
        _overlay.value = target
    }

    fun closeOverlay() {
        _overlay.value = Overlay.NONE
    }

    fun setTab(value: LibraryTab) = updateSettings { it.copy(libraryTab = value) }

    fun setSort(value: SortOrder) = updateSettings { it.copy(librarySort = value) }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        scope.launch { settingsRepository.update(transform) }
    }

    fun play(tracks: List<Track>, index: Int) {
        if (tracks.isEmpty()) return
        connection.playTracks(tracks, index.coerceIn(0, tracks.lastIndex))
    }

    fun playShuffled(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        connection.playTracks(tracks, 0, shuffle = true)
    }

    fun playNext(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        connection.playNext(tracks)
        _messages.tryEmit(queuedMessage("Queued next", tracks.size))
    }

    fun addToQueue(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        connection.addToQueue(tracks)
        _messages.tryEmit(queuedMessage("Added to queue", tracks.size))
    }

    fun addToPlaylist(playlistId: Long, tracks: List<Track>) {
        if (tracks.isEmpty()) return
        scope.launch {
            playlistRepository.addTracks(playlistId, tracks.map { it.id })
            _messages.tryEmit(queuedMessage("Added to playlist", tracks.size))
        }
    }

    fun createPlaylist(name: String, tracks: List<Track> = emptyList()) {
        scope.launch {
            val id = playlistRepository.create(name)
            if (tracks.isNotEmpty()) playlistRepository.addTracks(id, tracks.map { it.id })
            _messages.tryEmit("Created playlist \"${name.trim().ifEmpty { "Playlist" }}\"")
        }
    }

    fun renamePlaylist(id: Long, name: String) {
        scope.launch {
            playlistRepository.rename(id, name)
            _messages.tryEmit("Playlist renamed")
        }
    }

    fun deletePlaylist(id: Long) {
        scope.launch {
            playlistRepository.delete(id)
            if (_openPlaylistId.value == id) _openPlaylistId.value = null
            _messages.tryEmit("Playlist deleted")
        }
    }

    fun reorderPlaylists(ids: List<Long>) {
        scope.launch { playlistRepository.setPlaylistOrder(ids) }
    }

    fun reorderPlaylistTracks(id: Long, ids: List<Long>) {
        scope.launch { playlistRepository.reorder(id, ids) }
    }

    fun removeFromPlaylist(id: Long, index: Int) {
        scope.launch { playlistRepository.removeAt(id, index) }
    }

    fun openPlaylist(id: Long) {
        _openPlaylistId.value = id
    }

    fun closePlaylist() {
        _openPlaylistId.value = null
    }

    fun importM3u(uri: Uri) {
        scope.launch {
            val (_, count) = playlistRepository.importM3u(uri)
            _messages.tryEmit(if (count == 1) "Imported 1 track" else "Imported $count tracks")
        }
    }

    fun exportM3u(id: Long, uri: Uri) {
        scope.launch {
            runCatching { playlistRepository.exportM3u(id, uri) }
                .onSuccess { _messages.tryEmit("Playlist exported") }
                .onFailure { _messages.tryEmit("Export failed") }
        }
    }

    fun removeTracks(ids: List<Long>) {
        if (ids.isEmpty()) return
        scope.launch {
            libraryRepository.removeTracks(ids)
            _messages.tryEmit(if (ids.size == 1) "Removed 1 track" else "Removed ${ids.size} tracks")
        }
    }

    fun setManualOrder(ids: List<Long>) {
        if (ids.isEmpty()) return
        scope.launch { libraryRepository.setManualOrder(ids) }
    }

    fun importFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        scope.launch {
            val added = runCatching { libraryRepository.importFiles(uris) }.getOrDefault(0)
            _messages.tryEmit(if (added == 1) "Added 1 track" else "Added $added tracks")
        }
    }

    fun importFolder(uri: Uri) {
        scope.launch {
            val added = runCatching { libraryRepository.importFolder(uri) }.getOrDefault(0)
            _messages.tryEmit(if (added == 1) "Added 1 track" else "Added $added tracks")
        }
    }

    fun rescan() {
        libraryRepository.rescan()
    }

    fun setVolume(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        connection.setVolume(clamped)
        volumeJob?.cancel()
        volumeJob = scope.launch {
            delay(VOLUME_PERSIST_DEBOUNCE_MS)
            settingsRepository.update { it.copy(volume = clamped) }
        }
    }

    fun togglePlayPause() = connection.togglePlayPause()

    fun play() = connection.play()

    fun pause() = connection.pause()

    fun stop() = connection.stop()

    fun next() = connection.next()

    fun previous() = connection.previous()

    fun seekTo(ms: Long) = connection.seekTo(ms)

    fun toggleShuffle() = connection.toggleShuffle()

    fun cycleRepeat() = connection.cycleRepeat()

    fun skipToIndex(index: Int) = connection.skipToIndex(index)

    fun moveQueueItem(fromIndex: Int, toIndex: Int) = connection.moveQueueItem(fromIndex, toIndex)

    fun removeQueueItem(index: Int) = connection.removeQueueItem(index)

    fun clearUpNext() = connection.clearUpNext()

    fun setEqEnabled(enabled: Boolean) = equalizer.setEnabled(enabled)

    fun setEqBand(index: Int, millibels: Int) = equalizer.setBandLevel(index, millibels)

    fun applyEqPreset(name: String) = equalizer.applyPreset(name)

    fun setBassBoost(strength: Int) = equalizer.setBassBoost(strength)

    fun setVirtualizer(strength: Int) = equalizer.setVirtualizer(strength)

    fun startSleepTimer(minutes: Int) {
        sleep.start(minutes)
        _messages.tryEmit(if (minutes > 0) "Sleep timer set for $minutes min" else "Sleep timer off")
    }

    fun startSleepTimerEndOfTrack() {
        sleep.startEndOfTrack()
        _messages.tryEmit("Sleep timer set for end of track")
    }

    fun cancelSleepTimer() {
        sleep.cancel()
        _messages.tryEmit("Sleep timer off")
    }

    fun addCustomBackdrop(uri: Uri) {
        val value = uri.toString()
        scope.launch {
            runCatching {
                getApplication<Application>().contentResolver
                    .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            settingsRepository.update { current ->
                val list = if (current.customBackdrops.contains(value)) {
                    current.customBackdrops
                } else {
                    current.customBackdrops + value
                }
                current.copy(customBackdrops = list, backdropId = BACKDROP_CUSTOM_PREFIX + value)
            }
            _messages.tryEmit("Backdrop added")
        }
    }

    fun removeCustomBackdrop(uri: String) {
        scope.launch {
            settingsRepository.update { current ->
                current.copy(
                    customBackdrops = current.customBackdrops.filterNot { it == uri },
                    backdropId = if (current.backdropId == BACKDROP_CUSTOM_PREFIX + uri) {
                        BACKDROP_STATIC
                    } else {
                        current.backdropId
                    },
                )
            }
        }
    }

    private fun queuedMessage(prefix: String, count: Int): String =
        if (count == 1) "$prefix 1 track" else "$prefix $count tracks"

    private companion object {
        const val SUBSCRIPTION_MS = 5_000L
        const val QUERY_DEBOUNCE_MS = 200L
        const val VOLUME_PERSIST_DEBOUNCE_MS = 250L
        const val MESSAGE_BUFFER = 8
    }
}
