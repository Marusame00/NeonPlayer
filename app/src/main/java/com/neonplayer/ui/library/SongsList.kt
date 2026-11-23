package com.neonplayer.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neonplayer.domain.model.Track
import com.neonplayer.domain.model.formatDuration
import com.neonplayer.ui.theme.Neon
import com.neonplayer.ui.theme.NeonType
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyColumnState

@Composable
fun SongsList(
    songs: List<Track>,
    currentTrackId: Long?,
    isPlaying: Boolean,
    manualSort: Boolean,
    onPlay: (Int) -> Unit,
    actionsFor: (Track, Int) -> TrackMenuActions,
    onManualOrder: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = Neon.palette
    val listState = rememberLazyListState()
    val items = remember(songs) { songs.toMutableStateList() }
    val reorderState = rememberReorderableLazyColumnState(listState) { from, to ->
        items.add(to.index, items.removeAt(from.index))
    }
    var wasDragging by remember { mutableStateOf(false) }
    val totalDuration = remember(songs) { formatDuration(songs.sumOf { it.durationMs }) }

    LaunchedEffect(reorderState.isAnyItemDragging) {
        if (reorderState.isAnyItemDragging) {
            wasDragging = true
        } else if (wasDragging) {
            wasDragging = false
            onManualOrder(items.map { it.id })
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
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
                contentType = { _, _ -> "song" },
            ) { index, track ->
                if (manualSort) {
                    ReorderableItem(reorderState, key = track.id) { _ ->
                        TrackRow(
                            track = track,
                            index = index,
                            isCurrent = track.id == currentTrackId,
                            isPlaying = isPlaying,
                            onClick = { onPlay(index) },
                            actions = actionsFor(track, index),
                            trailing = {
                                Box(Modifier.draggableHandle()) {
                                    DragHandle()
                                }
                            },
                        )
                    }
                } else {
                    TrackRow(
                        track = track,
                        index = index,
                        isCurrent = track.id == currentTrackId,
                        isPlaying = isPlaying,
                        onClick = { onPlay(index) },
                        actions = actionsFor(track, index),
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, start = 2.dp, end = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (manualSort) "DRAG TO REORDER · HOLD FOR MORE" else "TAP TO PLAY · HOLD FOR MORE",
                style = NeonType.MonoSmall,
                color = palette.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(
                text = totalDuration,
                style = NeonType.MonoSmall,
                color = palette.textMuted,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
