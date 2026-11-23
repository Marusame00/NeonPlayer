package com.neonplayer.data.scan

import com.neonplayer.data.db.TrackEntity
import com.neonplayer.data.db.titleSortKey
import com.neonplayer.domain.model.TrackSource

data class ScannedTrack(
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
)

fun ScannedTrack.toEntity(id: Long, manualOrder: Int, dateAdded: Long = this.dateAdded): TrackEntity = TrackEntity(
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
    source = source.name,
    missing = false,
    manualOrder = manualOrder,
    titleSort = titleSortKey(title),
)
