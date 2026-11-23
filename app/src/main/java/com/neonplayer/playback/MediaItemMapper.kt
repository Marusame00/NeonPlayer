package com.neonplayer.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.neonplayer.domain.model.Track
import com.neonplayer.domain.model.TrackSource

private const val K_ID = "np_id"
private const val K_URI = "np_uri"
private const val K_ALBUM_ARTIST = "np_album_artist"
private const val K_TRACK_NO = "np_track_no"
private const val K_DISC_NO = "np_disc_no"
private const val K_YEAR = "np_year"
private const val K_DURATION = "np_duration"
private const val K_FORMAT = "np_format"
private const val K_MIME = "np_mime"
private const val K_FOLDER = "np_folder"
private const val K_DATE = "np_date"
private const val K_SIZE = "np_size"
private const val K_SOURCE = "np_source"

fun Track.toMediaItem(): MediaItem {
    val extras = Bundle().apply {
        putLong(K_ID, id)
        putString(K_URI, uri)
        putString(K_ALBUM_ARTIST, albumArtist)
        putInt(K_TRACK_NO, trackNumber)
        putInt(K_DISC_NO, discNumber)
        putInt(K_YEAR, year)
        putLong(K_DURATION, durationMs)
        putString(K_FORMAT, format)
        putString(K_MIME, mimeType)
        putString(K_FOLDER, folder)
        putLong(K_DATE, dateAdded)
        putLong(K_SIZE, sizeBytes)
        putString(K_SOURCE, source.name)
    }
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(displayArtist)
        .setAlbumTitle(album)
        .setAlbumArtist(albumArtist)
        .setTrackNumber(trackNumber.takeIf { it > 0 })
        .setRecordingYear(year.takeIf { it > 0 })
        .setDurationMs(durationMs.takeIf { it > 0 })
        .setIsPlayable(true)
        .setIsBrowsable(false)
        .setExtras(extras)
        .build()
    val parsed = Uri.parse(uri)
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(parsed)
        .setMimeType(mimeType.ifBlank { null })
        .setMediaMetadata(metadata)
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(parsed).setExtras(extras).build())
        .build()
}

fun MediaItem.ensurePlayableUri(): MediaItem {
    if (localConfiguration != null) return this
    val uri = requestMetadata.mediaUri
        ?: mediaMetadata.extras?.getString(K_URI)?.let(Uri::parse)
        ?: requestMetadata.extras?.getString(K_URI)?.let(Uri::parse)
        ?: return this
    return buildUpon().setUri(uri).build()
}

fun MediaItem.toTrackOrNull(): Track? {
    val e = mediaMetadata.extras ?: requestMetadata.extras ?: return null
    val id = e.getLong(K_ID, -1L).takeIf { it >= 0 } ?: mediaId.toLongOrNull() ?: return null
    return Track(
        id = id,
        uri = e.getString(K_URI) ?: requestMetadata.mediaUri?.toString() ?: localConfiguration?.uri?.toString() ?: "",
        title = mediaMetadata.title?.toString().orEmpty(),
        artist = mediaMetadata.artist?.toString().orEmpty(),
        album = mediaMetadata.albumTitle?.toString().orEmpty(),
        albumArtist = e.getString(K_ALBUM_ARTIST).orEmpty(),
        trackNumber = e.getInt(K_TRACK_NO),
        discNumber = e.getInt(K_DISC_NO),
        year = e.getInt(K_YEAR),
        durationMs = e.getLong(K_DURATION),
        format = e.getString(K_FORMAT).orEmpty(),
        mimeType = e.getString(K_MIME).orEmpty(),
        folder = e.getString(K_FOLDER).orEmpty(),
        dateAdded = e.getLong(K_DATE),
        sizeBytes = e.getLong(K_SIZE),
        source = runCatching { TrackSource.valueOf(e.getString(K_SOURCE) ?: "") }.getOrDefault(TrackSource.MEDIASTORE),
    )
}
