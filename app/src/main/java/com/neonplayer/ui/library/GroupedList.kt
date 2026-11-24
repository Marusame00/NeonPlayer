package com.neonplayer.ui.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.neonplayer.R
import com.neonplayer.domain.model.Track
import com.neonplayer.ui.components.CountBadge
import com.neonplayer.ui.theme.Neon
import com.neonplayer.ui.theme.NeonType

@Composable
fun <G> GroupedList(
    groups: List<G>,
    groupKey: (G) -> String,
    groupTitle: (G) -> String,
    groupSubtitle: (G) -> String?,
    groupTracks: (G) -> List<Track>,
    currentTrackId: Long?,
    isPlaying: Boolean,
    onPlayGroup: (G, Int) -> Unit,
    actionsForGroup: (G) -> TrackMenuActions,
    actionsForTrack: (G, Track, Int) -> TrackMenuActions,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val expandedKeys = rememberSaveable(
        stateSaver = listSaver<Set<String>, String>(
            save = { it.toList() },
            restore = { it.toSet() },
        ),
    ) { mutableStateOf(emptySet<String>()) }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        groups.forEach { group ->
            val key = groupKey(group)
            val expanded = key in expandedKeys.value
            item(key = "g:$key", contentType = "groupHeader") {
                GroupHeader(
                    title = groupTitle(group),
                    subtitle = groupSubtitle(group),
                    count = groupTracks(group).size,
                    expanded = expanded,
                    onToggle = {
                        expandedKeys.value = if (expanded) {
                            expandedKeys.value - key
                        } else {
                            expandedKeys.value + key
                        }
                    },
                    actions = actionsForGroup(group),
                )
            }
            if (expanded) {
                val tracks = groupTracks(group)
                itemsIndexed(
                    items = tracks,
                    key = { index, track -> "t:$key:$index:${track.id}" },
                    contentType = { _, _ -> "track" },
                ) { index, track ->
                    TrackRow(
                        track = track,
                        index = index,
                        isCurrent = track.id == currentTrackId,
                        isPlaying = isPlaying,
                        onClick = { onPlayGroup(group, index) },
                        actions = actionsForTrack(group, track, index),
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(
    title: String,
    subtitle: String?,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    actions: TrackMenuActions,
) {
    val palette = Neon.palette
    val reduceMotion = Neon.style.reduceMotion
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        animationSpec = if (reduceMotion) snap() else tween(180),
        label = "groupChevron",
    )
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(palette.primary.copy(alpha = 0.06f), shape)
                .border(1.dp, palette.border, shape)
                .drawBehind {
                    drawRect(
                        color = palette.primary,
                        topLeft = Offset.Zero,
                        size = Size(2.dp.toPx(), size.height),
                    )
                }
                .combinedClickable(
                    onClick = onToggle,
                    onLongClick = { menuOpen = true },
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = stringResource(
                    if (expanded) R.string.action_collapse else R.string.action_expand,
                ),
                tint = palette.primary,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { rotationZ = rotation },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = NeonType.TitleM,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = subtitle,
                        style = NeonType.BodySmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(2.dp))
            CountBadge(count)
        }
        TrackContextMenu(
            expanded = menuOpen,
            onDismiss = { menuOpen = false },
            actions = actions,
        )
    }
}
