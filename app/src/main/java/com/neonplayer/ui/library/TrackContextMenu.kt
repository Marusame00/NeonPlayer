package com.neonplayer.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neonplayer.domain.model.Playlist
import com.neonplayer.domain.model.Track
import com.neonplayer.domain.model.formatDuration
import com.neonplayer.ui.components.CountBadge
import com.neonplayer.ui.components.NeonTextButton
import com.neonplayer.ui.theme.Neon
import com.neonplayer.ui.theme.NeonShapes
import com.neonplayer.ui.theme.NeonType

@Immutable
data class TrackMenuActions(
    val onPlayNext: () -> Unit,
    val onAddToQueue: () -> Unit,
    val onAddToPlaylist: () -> Unit,
    val onPlay: (() -> Unit)? = null,
    val onInfo: (() -> Unit)? = null,
    val onRemove: (() -> Unit)? = null,
    val removeLabel: String = "Remove from library",
)

@Composable
fun TrackContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    actions: TrackMenuActions,
    modifier: Modifier = Modifier,
) {
    val palette = Neon.palette
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = palette.surfaceHigh,
        shape = RoundedCornerShape(NeonShapes.SmallRadius),
    ) {
        val play = actions.onPlay
        if (play != null) {
            MenuEntry("Play", Icons.Rounded.PlayArrow) {
                onDismiss()
                play()
            }
        }
        MenuEntry("Play next", Icons.Rounded.SkipNext) {
            onDismiss()
            actions.onPlayNext()
        }
        MenuEntry("Add to queue", Icons.Rounded.QueueMusic) {
            onDismiss()
            actions.onAddToQueue()
        }
        MenuEntry("Add to playlist", Icons.Rounded.PlaylistAdd) {
            onDismiss()
            actions.onAddToPlaylist()
        }
        val info = actions.onInfo
        if (info != null) {
            MenuEntry("Track info", Icons.Rounded.Info) {
                onDismiss()
                info()
            }
        }
        val remove = actions.onRemove
        if (remove != null) {
            MenuEntry(actions.removeLabel, Icons.Rounded.Delete, danger = true) {
                onDismiss()
                remove()
            }
        }
    }
}

@Composable
private fun MenuEntry(
    label: String,
    icon: ImageVector,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val palette = Neon.palette
    val tint = if (danger) palette.textSecondary else palette.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
        Text(text = label, style = NeonType.Body, color = palette.textPrimary)
    }
}

@Composable
fun NeonDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String = "Cancel",
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = Neon.palette
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = palette.surfaceHigh,
        shape = RoundedCornerShape(NeonShapes.CardRadius),
        title = {
            Text(text = title.uppercase(), style = NeonType.Label, color = palette.primary)
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                content = content,
            )
        },
        confirmButton = {
            NeonTextButton(
                text = confirmLabel,
                onClick = onConfirm,
                primary = confirmEnabled,
            )
        },
        dismissButton = {
            NeonTextButton(text = dismissLabel, onClick = onDismiss)
        },
    )
}

@Composable
fun NeonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    val palette = Neon.palette
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = NeonType.Body,
        placeholder = {
            Text(text = placeholder, style = NeonType.Body, color = palette.textMuted)
        },
        shape = RoundedCornerShape(NeonShapes.SmallRadius),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = palette.textPrimary,
            unfocusedTextColor = palette.textPrimary,
            cursorColor = palette.primary,
            focusedBorderColor = palette.primary,
            unfocusedBorderColor = palette.border,
            focusedContainerColor = palette.base1,
            unfocusedContainerColor = palette.base1,
        ),
    )
}

@Composable
fun TrackInfoDialog(track: Track, onDismiss: () -> Unit) {
    val palette = Neon.palette
    NeonDialog(
        title = "Track info",
        onDismiss = onDismiss,
        confirmLabel = "Close",
        onConfirm = onDismiss,
        dismissLabel = "Dismiss",
    ) {
        InfoRow("Title", track.title)
        InfoRow("Artist", track.displayArtist)
        InfoRow("Album", track.displayAlbum)
        InfoRow("Album artist", track.groupArtist)
        InfoRow("Track #", if (track.trackNumber > 0) track.trackNumber.toString() else "—")
        InfoRow("Disc #", if (track.discNumber > 0) track.discNumber.toString() else "—")
        InfoRow("Year", if (track.year > 0) track.year.toString() else "—")
        InfoRow("Duration", formatDuration(track.durationMs))
        InfoRow("Format", track.format.ifBlank { "—" })
        InfoRow("Mime", track.mimeType.ifBlank { "—" })
        InfoRow("Size", formatSize(track.sizeBytes))
        InfoRow("Folder", track.folder.ifBlank { "—" })
        InfoRow("URI", track.uri)
        InfoRow("Date added", formatDate(track.dateAdded))
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Edit tags display is read-only.",
            style = NeonType.BodySmall,
            color = palette.textMuted,
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val palette = Neon.palette
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label.uppercase(), style = NeonType.Label, color = palette.textMuted)
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = NeonType.BodySmall,
            color = palette.textPrimary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
    onCreate: () -> Unit,
) {
    val palette = Neon.palette
    NeonDialog(
        title = "Add to playlist",
        onDismiss = onDismiss,
        confirmLabel = "New playlist",
        onConfirm = onCreate,
        dismissLabel = "Cancel",
    ) {
        if (playlists.isEmpty()) {
            Text(
                text = "No playlists yet.",
                style = NeonType.BodySmall,
                color = palette.textMuted,
            )
        } else {
            playlists.forEach { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(playlist.id) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = playlist.name,
                        style = NeonType.Body,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    CountBadge(playlist.trackCount)
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return "—"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) "%.1f MB".format(mb) else "%.0f KB".format(bytes / 1024.0)
}

private fun formatDate(epochMs: Long): String {
    if (epochMs <= 0L) return "—"
    return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
        .format(java.util.Date(epochMs))
}
