package com.neonplayer.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.neonplayer.domain.model.Track
import com.neonplayer.domain.model.TrackSource
import java.util.Locale

@Entity(
    tableName = "tracks",
    indices = [
        Index(value = ["uri"], unique = true),
        Index(value = ["titleSort"]),
        Index(value = ["artist"]),
        Index(value = ["album"]),
        Index(value = ["dateAdded"]),
        Index(value = ["durationMs"]),
        Index(value = ["manualOrder"]),
        Index(value = ["folder"]),
    ],
)
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
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
    val source: String,
    val missing: Boolean,
    val manualOrder: Int,
    val titleSort: String,
)

fun TrackEntity.toTrack(): Track = Track(
    id = id,
    uri = uri,
    title = title,
    artist = artist,
    album = album,
    albumArtist = albumArtist,
    trackNumber = trackNumber,
    discNumber = discNumber,
    year = year,
    durationMs = durationMs,
    format = format,
    mimeType = mimeType,
    folder = folder,
    dateAdded = dateAdded,
    sizeBytes = sizeBytes,
    source = runCatching { TrackSource.valueOf(source) }.getOrDefault(TrackSource.MEDIASTORE),
    missing = missing,
)

fun titleSortKey(title: String): String = title.trim().lowercase(Locale.ROOT)
