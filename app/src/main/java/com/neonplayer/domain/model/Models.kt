package com.neonplayer.domain.model

import androidx.compose.runtime.Immutable

const val UNKNOWN_ARTIST = "Unknown artist"
const val UNKNOWN_ALBUM = "Unknown album"

enum class TrackSource { MEDIASTORE, SAF }

@Immutable
data class Track(
    val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val trackNumber: Int,
    val discNumber: Int,
    val year: Int,
    val durationMs: Long,
    val format: String,
    val mimeType: String,
    val folder: String,
    val dateAdded: Long,
    val sizeBytes: Long,
    val source: TrackSource,
    val missing: Boolean = false,
) {
    val displayArtist: String get() = artist.ifBlank { UNKNOWN_ARTIST }
    val displayAlbum: String get() = album.ifBlank { UNKNOWN_ALBUM }
    val groupArtist: String get() = albumArtist.ifBlank { displayArtist }
}

@Immutable
data class AlbumGroup(
    val key: String,
    val title: String,
    val artist: String,
    val year: Int,
    val tracks: List<Track>,
)

@Immutable
data class ArtistGroup(
    val name: String,
    val tracks: List<Track>,
)

@Immutable
data class FolderGroup(
    val path: String,
    val name: String,
    val tracks: List<Track>,
)

@Immutable
data class Playlist(
    val id: Long,
    val name: String,
    val trackCount: Int,
    val totalDurationMs: Long,
    val position: Int,
    val createdAt: Long,
)

enum class SortOrder(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DATE_ADDED("Date added"),
    DURATION("Duration"),
    MANUAL("Manual"),
}

enum class LibraryTab(val label: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists"),
    FOLDERS("Folders"),
}

enum class RepeatMode { OFF, ALL, ONE }

sealed interface ScanState {
    data object Idle : ScanState
    data class Scanning(val processed: Int, val total: Int) : ScanState
    data class Done(val added: Int, val removed: Int, val total: Int) : ScanState
    data class Failed(val message: String) : ScanState
}

fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
