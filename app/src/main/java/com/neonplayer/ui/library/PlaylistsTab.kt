package com.neonplayer.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.neonplayer.R
import com.neonplayer.domain.model.Playlist
import com.neonplayer.domain.model.Track
import com.neonplayer.domain.model.formatDuration
import com.neonplayer.ui.components.CountBadge
import com.neonplayer.ui.components.NeonIconButton
import com.neonplayer.ui.components.NeonTextButton
import com.neonplayer.ui.theme.Neon
import com.neonplayer.ui.theme.NeonType
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyColumnState

@Composable
fun PlaylistsTab(
    playlists: List<Playlist>,
    openPlaylistId: Long?,
    openPlaylistTracks: List<Track>,
    currentTrackId: Long?,
    isPlaying: Boolean,
    onOpen: (Long) -> Unit,
    onClose: () -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    onPlayShuffled: (List<Track>) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onExport: (Long, Uri) -> Unit,
    onImportM3u: (Uri) -> Unit,
    onReorderPlaylists: (List<Long>) -> Unit,
    onReorderTracks: (Long, List<Long>) -> Unit,
    actionsFor: (Track, Int) -> TrackMenuActions,
    modifier: Modifier = Modifier,
) {
    val open = playlists.firstOrNull { it.id == openPlaylistId }
    if (openPlaylistId != null && open != null) {
        PlaylistDetail(
            playlist = open,
            tracks = openPlaylistTracks,
            currentTrackId = currentTrackId,
            isPlaying = isPlaying,
            onClose = onClose,
            onPlay = onPlay,
            onPlayShuffled = onPlayShuffled,
            onRename = onRename,
            onDelete = onDelete,
            onExport = onExport,
            onReorderTracks = onReorderTracks,
            actionsFor = actionsFor,
            modifier = modifier,
        )
    } else {
        PlaylistList(
            playlists = playlists,
            onOpen = onOpen,
            onCreate = onCreate,
            onImportM3u = onImportM3u,
            onReorderPlaylists = onReorderPlaylists,
            modifier = modifier,
        )
    }
}

@Composable
private fun PlaylistList(
    playlists: List<Playlist>,
    onOpen: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onImportM3u: (Uri) -> Unit,
    onReorderPlaylists: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Neon.palette
    val listState = rememberLazyListState()
    val items = remember(playlists) { playlists.toMutableStateList() }
    val reorderState = rememberReorderableLazyColumnState(listState) { from, to ->
        items.add(to.index, items.removeAt(from.index))
    }
    var wasDragging by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var draftName by remember { mutableStateOf("") }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) onImportM3u(uri)
    }

    LaunchedEffect(reorderState.isAnyItemDragging) {
        if (reorderState.isAnyItemDragging) {
            wasDragging = true
        } else if (wasDragging) {
            wasDragging = false
            onReorderPlaylists(items.map { it.id })
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeonTextButton(
                text = stringResource(R.string.playlists_new),
                icon = Icons.Rounded.PlayArrow,
                primary = true,
                onClick = {
                    draftName = ""
                    creating = true
                },
            )
            NeonTextButton(
                text = stringResource(R.string.playlists_import_m3u),
                icon = Icons.Rounded.Save,
                onClick = {
                    importLauncher.launch(arrayOf("audio/x-mpegurl", "audio/mpegurl", "*/*"))
                },
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            itemsIndexed(
                items = items,
                key = { _, playlist -> playlist.id },
                contentType = { _, _ -> "playlist" },
            ) { index, playlist ->
                ReorderableItem(reorderState, key = playlist.id) { _ ->
                    PlaylistRow(
                        playlist = playlist,
                        index = index,
                        onClick = { onOpen(playlist.id) },
                        trailing = {
                            Box(Modifier.draggableHandle()) {
                                DragHandle()
                            }
                        },
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.playlists_hold_drag),
            style = NeonType.MonoSmall,
            color = palette.textMuted,
        )
    }

    if (creating) {
        NeonDialog(
            title = stringResource(R.string.playlists_new_title),
            onDismiss = { creating = false },
            confirmLabel = stringResource(R.string.action_create),
            onConfirm = {
                creating = false
                onCreate(draftName)
            },
        ) {
            NeonTextField(
                value = draftName,
                onValueChange = { draftName = it },
                placeholder = stringResource(R.string.playlists_name_placeholder),
            )
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    index: Int,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    val palette = Neon.palette
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(shape)
            .background(palette.surfaceHigh.copy(alpha = 0.45f), shape)
            .border(1.dp, palette.border, shape)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = (index + 1).toString().padStart(2, '0'),
            style = NeonType.MonoSmall,
            color = palette.textMuted,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = playlist.name,
                style = NeonType.Body,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = formatDuration(playlist.totalDurationMs),
                style = NeonType.MonoSmall,
                color = palette.textSecondary,
            )
        }
        CountBadge(playlist.trackCount)
        if (trailing != null) trailing()
    }
}

@Composable
private fun PlaylistDetail(
    playlist: Playlist,
    tracks: List<Track>,
    currentTrackId: Long?,
    isPlaying: Boolean,
    onClose: () -> Unit,
    onPlay: (List<Track>, Int) -> Unit,
    onPlayShuffled: (List<Track>) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onExport: (Long, Uri) -> Unit,
    onReorderTracks: (Long, List<Long>) -> Unit,
    actionsFor: (Track, Int) -> TrackMenuActions,
    modifier: Modifier = Modifier,
) {
    val palette = Neon.palette
    val listState = rememberLazyListState()
    val items = remember(tracks) { tracks.toMutableStateList() }
    val reorderState = rememberReorderableLazyColumnState(listState) { from, to ->
        items.add(to.index, items.removeAt(from.index))
    }
    var wasDragging by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var draftName by remember { mutableStateOf(playlist.name) }
    var deleting by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl"),
    ) { uri ->
        if (uri != null) onExport(playlist.id, uri)
    }

    LaunchedEffect(reorderState.isAnyItemDragging) {
        if (reorderState.isAnyItemDragging) {
            wasDragging = true
        } else if (wasDragging) {
            wasDragging = false
            onReorderTracks(playlist.id, items.map { it.id })
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            NeonIconButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.playlists_back),
                onClick = onClose,
                size = 34.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = NeonType.TitleM,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = LocalContext.current.getString(
                        R.string.playlists_detail_count,
                        playlist.trackCount,
                        formatDuration(playlist.totalDurationMs),
                    ),
                    style = NeonType.MonoSmall,
                    color = palette.textMuted,
                )
            }
            CountBadge(playlist.trackCount)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeonIconButton(
                icon = Icons.Rounded.PlayArrow,
                contentDescription = stringResource(R.string.playlists_play),
                onClick = { if (tracks.isNotEmpty()) onPlay(tracks, 0) },
                size = 34.dp,
                active = true,
            )
            NeonIconButton(
                icon = Icons.Rounded.Shuffle,
                contentDescription = stringResource(R.string.playlists_shuffle),
                onClick = { onPlayShuffled(tracks) },
                size = 34.dp,
            )
            NeonIconButton(
                icon = Icons.Rounded.Edit,
                contentDescription = stringResource(R.string.playlists_rename),
                onClick = {
                    draftName = playlist.name
                    renaming = true
                },
                size = 34.dp,
            )
            NeonIconButton(
                icon = Icons.Rounded.Save,
                contentDescription = stringResource(R.string.playlists_export),
                onClick = { exportLauncher.launch("${playlist.name}.m3u") },
                size = 34.dp,
            )
            NeonIconButton(
                icon = Icons.Rounded.Delete,
                contentDescription = stringResource(R.string.playlists_delete),
                onClick = { deleting = true },
                size = 34.dp,
                tint = palette.textSecondary,
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            itemsIndexed(
                items = items,
                key = { _, track -> track.id },
                contentType = { _, _ -> "playlistTrack" },
            ) { index, track ->
                ReorderableItem(reorderState, key = track.id) { _ ->
                    TrackRow(
                        track = track,
                        index = index,
                        isCurrent = track.id == currentTrackId,
                        isPlaying = isPlaying,
                        onClick = { onPlay(items.toList(), index) },
                        actions = actionsFor(track, index),
                        trailing = {
                            Box(Modifier.draggableHandle()) {
                                DragHandle()
                            }
                        },
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.playlists_hold_track),
            style = NeonType.MonoSmall,
            color = palette.textMuted,
        )
    }

    if (renaming) {
        NeonDialog(
            title = stringResource(R.string.playlists_rename),
            onDismiss = { renaming = false },
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = {
                renaming = false
                onRename(playlist.id, draftName)
            },
        ) {
            NeonTextField(
                value = draftName,
                onValueChange = { draftName = it },
                placeholder = stringResource(R.string.playlists_name_placeholder),
            )
        }
    }

    if (deleting) {
        NeonDialog(
            title = stringResource(R.string.playlists_delete),
            onDismiss = { deleting = false },
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                deleting = false
                onDelete(playlist.id)
            },
        ) {
            Text(
                text = stringResource(R.string.playlists_delete_msg, playlist.name),
                style = NeonType.BodySmall,
                color = palette.textSecondary,
            )
        }
    }
}
