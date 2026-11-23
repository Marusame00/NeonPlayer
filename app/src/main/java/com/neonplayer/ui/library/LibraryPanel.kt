package com.neonplayer.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neonplayer.domain.model.LibraryTab
import com.neonplayer.domain.model.ScanState
import com.neonplayer.domain.model.SortOrder
import com.neonplayer.domain.model.Track
import com.neonplayer.domain.model.formatDuration
import com.neonplayer.ui.components.CountBadge
import com.neonplayer.ui.components.EmptyState
import com.neonplayer.ui.components.NeonCard
import com.neonplayer.ui.components.NeonDropdown
import com.neonplayer.ui.components.NeonIconButton
import com.neonplayer.ui.components.NeonSearchField
import com.neonplayer.ui.components.NeonTabs
import com.neonplayer.ui.components.SectionLabel
import com.neonplayer.ui.main.MainViewModel
import com.neonplayer.ui.theme.Neon
import com.neonplayer.ui.theme.NeonType

@Composable
fun LibraryPanel(
    vm: MainViewModel,
    audioPermissionGranted: Boolean,
    onGrantAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Neon.palette
    val reduceMotion = Neon.style.reduceMotion
    val tab by vm.tab.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val songs by vm.songs.collectAsStateWithLifecycle()
    val albums by vm.albums.collectAsStateWithLifecycle()
    val artists by vm.artists.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val trackCount by vm.trackCount.collectAsStateWithLifecycle()
    val scanState by vm.scanState.collectAsStateWithLifecycle()
    val player by vm.player.collectAsStateWithLifecycle()
    val openPlaylistId by vm.openPlaylistId.collectAsStateWithLifecycle()
    val openPlaylistTracks by vm.openPlaylistTracks.collectAsStateWithLifecycle()

    val infoTrack = remember { mutableStateOf<Track?>(null) }
    val addTarget = remember { mutableStateOf<List<Track>?>(null) }
    val newPlaylistTracks = remember { mutableStateOf<List<Track>?>(null) }
    var newPlaylistName by remember { mutableStateOf("") }

    val filesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris -> vm.importFiles(uris) }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> if (uri != null) vm.importFolder(uri) }

    val makeActions = remember(vm) {
        val factory: (Track, Int) -> TrackMenuActions = { track, _ ->
            TrackMenuActions(
                onPlayNext = { vm.playNext(listOf(track)) },
                onAddToQueue = { vm.addToQueue(listOf(track)) },
                onAddToPlaylist = { addTarget.value = listOf(track) },
                onInfo = { infoTrack.value = track },
                onRemove = { vm.removeTracks(listOf(track.id)) },
            )
        }
        factory
    }

    val groupActions = remember(vm) {
        val factory: (List<Track>) -> TrackMenuActions = { tracks ->
            TrackMenuActions(
                onPlayNext = { vm.playNext(tracks) },
                onAddToQueue = { vm.addToQueue(tracks) },
                onAddToPlaylist = { addTarget.value = tracks },
                onPlay = { if (tracks.isNotEmpty()) vm.play(tracks, 0) },
            )
        }
        factory
    }

    val currentTrackId = player.currentTrack?.id
    val isPlaying = player.isPlaying
    val scanning = scanState is ScanState.Scanning

    NeonCard(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionLabel(text = "Library")
            CountBadge(trackCount)
            Spacer(Modifier.weight(1f))
            NeonIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = "Add files",
                onClick = { filesLauncher.launch(arrayOf("audio/*")) },
                size = 32.dp,
            )
            NeonIconButton(
                icon = Icons.Rounded.CreateNewFolder,
                contentDescription = "Add folder",
                onClick = { folderLauncher.launch(null) },
                size = 32.dp,
            )
            RescanButton(scanning = scanning, reduceMotion = reduceMotion, onClick = vm::rescan)
        }
        Spacer(Modifier.height(4.dp))
        NeonTabs(
            tabs = LibraryTab.entries.map { it.label },
            selectedIndex = LibraryTab.entries.indexOf(tab),
            onSelect = { vm.setTab(LibraryTab.entries[it]) },
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NeonSearchField(
                value = query,
                onValueChange = { vm.query.value = it },
                placeholder = "Filter library",
                modifier = Modifier.weight(1f),
            )
            if (tab == LibraryTab.SONGS) {
                NeonDropdown(
                    selected = sort,
                    options = SortOrder.entries,
                    label = { it.label },
                    onSelect = { vm.setSort(it) },
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        if (!audioPermissionGranted && trackCount == 0) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    title = "NO SIGNAL",
                    message = "NeonPlayer needs access to your music to fill the grid. " +
                        "You can also add files or folders manually.",
                    actionLabel = "Grant access",
                    onAction = onGrantAccess,
                    secondaryActionLabel = "Add folder",
                    onSecondaryAction = { folderLauncher.launch(null) },
                )
            }
        } else if (tab == LibraryTab.PLAYLISTS) {
            PlaylistsTab(
                playlists = playlists,
                openPlaylistId = openPlaylistId,
                openPlaylistTracks = openPlaylistTracks,
                currentTrackId = currentTrackId,
                isPlaying = isPlaying,
                onOpen = vm::openPlaylist,
                onClose = vm::closePlaylist,
                onPlay = vm::play,
                onPlayShuffled = vm::playShuffled,
                onCreate = { name -> vm.createPlaylist(name) },
                onRename = vm::renamePlaylist,
                onDelete = vm::deletePlaylist,
                onExport = vm::exportM3u,
                onImportM3u = vm::importM3u,
                onReorderPlaylists = vm::reorderPlaylists,
                onReorderTracks = vm::reorderPlaylistTracks,
                actionsFor = makeActions,
                modifier = Modifier.weight(1f),
            )
        } else {
            val empty = when (tab) {
                LibraryTab.SONGS -> songs.isEmpty()
                LibraryTab.ALBUMS -> albums.isEmpty()
                LibraryTab.ARTISTS -> artists.isEmpty()
                LibraryTab.FOLDERS -> folders.isEmpty()
                LibraryTab.PLAYLISTS -> false
            }
            if (empty) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (query.isNotBlank()) {
                        Text(
                            text = "No matches for \"$query\"",
                            style = NeonType.BodySmall,
                            color = palette.textMuted,
                        )
                    } else {
                        EmptyState(
                            title = "THE GRID IS EMPTY",
                            message = "No music found yet. Scan your device or add files and folders.",
                            actionLabel = "Rescan",
                            onAction = vm::rescan,
                            secondaryActionLabel = "Add folder",
                            onSecondaryAction = { folderLauncher.launch(null) },
                        )
                    }
                }
            } else {
                when (tab) {
                    LibraryTab.SONGS -> SongsList(
                        songs = songs,
                        currentTrackId = currentTrackId,
                        isPlaying = isPlaying,
                        manualSort = sort == SortOrder.MANUAL,
                        onPlay = { index -> vm.play(songs, index) },
                        actionsFor = makeActions,
                        onManualOrder = vm::setManualOrder,
                        modifier = Modifier.weight(1f),
                    )

                    LibraryTab.ALBUMS -> GroupedList(
                        groups = albums,
                        groupKey = { it.key },
                        groupTitle = { it.title },
                        groupSubtitle = { album ->
                            if (album.year > 0) "${album.artist} · ${album.year}" else album.artist
                        },
                        groupTracks = { it.tracks },
                        currentTrackId = currentTrackId,
                        isPlaying = isPlaying,
                        onPlayGroup = { album, index -> vm.play(album.tracks, index) },
                        actionsForGroup = { groupActions(it.tracks) },
                        actionsForTrack = { _, track, _ -> makeActions(track, 0) },
                        modifier = Modifier.weight(1f),
                    )

                    LibraryTab.ARTISTS -> GroupedList(
                        groups = artists,
                        groupKey = { it.name },
                        groupTitle = { it.name },
                        groupSubtitle = { "${it.tracks.size} tracks" },
                        groupTracks = { it.tracks },
                        currentTrackId = currentTrackId,
                        isPlaying = isPlaying,
                        onPlayGroup = { artist, index -> vm.play(artist.tracks, index) },
                        actionsForGroup = { groupActions(it.tracks) },
                        actionsForTrack = { _, track, _ -> makeActions(track, 0) },
                        modifier = Modifier.weight(1f),
                    )

                    LibraryTab.FOLDERS -> GroupedList(
                        groups = folders,
                        groupKey = { it.path },
                        groupTitle = { it.name },
                        groupSubtitle = { formatDuration(it.tracks.sumOf { track -> track.durationMs }) },
                        groupTracks = { it.tracks },
                        currentTrackId = currentTrackId,
                        isPlaying = isPlaying,
                        onPlayGroup = { folder, index -> vm.play(folder.tracks, index) },
                        actionsForGroup = { groupActions(it.tracks) },
                        actionsForTrack = { _, track, _ -> makeActions(track, 0) },
                        modifier = Modifier.weight(1f),
                    )

                    LibraryTab.PLAYLISTS -> Unit
                }
            }
        }
    }

    val info = infoTrack.value
    if (info != null) {
        TrackInfoDialog(track = info, onDismiss = { infoTrack.value = null })
    }

    val target = addTarget.value
    if (target != null) {
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { addTarget.value = null },
            onPick = { playlistId ->
                vm.addToPlaylist(playlistId, target)
                addTarget.value = null
            },
            onCreate = {
                addTarget.value = null
                newPlaylistName = ""
                newPlaylistTracks.value = target
            },
        )
    }

    val pendingTracks = newPlaylistTracks.value
    if (pendingTracks != null) {
        NeonDialog(
            title = "New playlist",
            onDismiss = { newPlaylistTracks.value = null },
            confirmLabel = "Create",
            onConfirm = {
                vm.createPlaylist(newPlaylistName, pendingTracks)
                newPlaylistTracks.value = null
            },
        ) {
            NeonTextField(
                value = newPlaylistName,
                onValueChange = { newPlaylistName = it },
                placeholder = "Playlist name",
            )
        }
    }
}

@Composable
private fun RescanButton(
    scanning: Boolean,
    reduceMotion: Boolean,
    onClick: () -> Unit,
) {
    val rotation = if (scanning && !reduceMotion) {
        val transition = rememberInfiniteTransition(label = "rescanSpin")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
            ),
            label = "rescanAngle",
        )
        value
    } else {
        0f
    }
    Box(
        modifier = Modifier
            .size(34.dp)
            .graphicsLayer { rotationZ = rotation },
    ) {
        NeonIconButton(
            icon = Icons.Rounded.Refresh,
            contentDescription = "Rescan library",
            onClick = onClick,
            size = 34.dp,
            active = scanning,
        )
    }
}

@Composable
fun ScanStatusText(scanState: ScanState, modifier: Modifier = Modifier) {
    val palette = Neon.palette
    val text = when (scanState) {
        is ScanState.Idle -> "Idle"
        is ScanState.Scanning -> "Scanning ${scanState.processed} / ${scanState.total}"
        is ScanState.Done -> "Added ${scanState.added} · removed ${scanState.removed} · ${scanState.total} total"
        is ScanState.Failed -> "Failed: ${scanState.message}"
    }
    Text(
        text = text,
        style = NeonType.BodySmall,
        color = palette.textSecondary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
