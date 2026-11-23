package com.neonplayer.data.scan

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.neonplayer.domain.model.TrackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TagReader(private val context: Context) {

    suspend fun read(uri: Uri, folder: String = ""): ScannedTrack = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var albumArtist: String? = null
        var trackNumber = 0
        var discNumber = 0
        var year = 0
        var duration = 0L
        var mime: String? = null
        try {
            retriever.setDataSource(context, uri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            }
            trackNumber = parseLeadingInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER))
            discNumber = parseLeadingInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER))
            year = parseYear(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE),
            )
            duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        } catch (_: Throwable) {
        } finally {
            runCatching { retriever.release() }
        }

        val name = displayName(uri)
        val resolvedMime = mime ?: runCatching { context.contentResolver.getType(uri) }.getOrNull().orEmpty()
        val resolvedTitle = title?.takeIf { it.isNotBlank() } ?: stripExtension(name)

        ScannedTrack(
            uri = uri.toString(),
            title = resolvedTitle,
            artist = artist.orEmpty().let { if (it == UNKNOWN_ARTIST_TAG) "" else it },
            album = album.orEmpty(),
            albumArtist = albumArtist.orEmpty(),
            trackNumber = trackNumber,
            discNumber = discNumber,
            year = year,
            durationMs = duration,
            format = AudioFormat.of(name, resolvedMime),
            mimeType = resolvedMime,
            folder = folder,
            dateAdded = System.currentTimeMillis(),
            sizeBytes = sizeOf(uri),
            source = TrackSource.SAF,
        )
    }

    private fun displayName(uri: Uri): String {
        val fromDocument = runCatching { DocumentFile.fromSingleUri(context, uri)?.name }.getOrNull()
        if (!fromDocument.isNullOrBlank()) return fromDocument
        val fromProvider = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        if (!fromProvider.isNullOrBlank()) return fromProvider
        return uri.lastPathSegment.orEmpty()
    }

    private fun sizeOf(uri: Uri): Long = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else 0L
        } ?: 0L
    }.getOrDefault(0L)

    private fun parseLeadingInt(raw: String?): Int {
        if (raw.isNullOrBlank()) return 0
        return raw.substringBefore('/').trim().toIntOrNull() ?: 0
    }

    private fun parseYear(yearRaw: String?, dateRaw: String?): Int {
        yearRaw?.trim()?.toIntOrNull()?.let { if (it > 0) return it }
        if (dateRaw.isNullOrBlank()) return 0
        val digits = dateRaw.trim().take(4)
        return digits.toIntOrNull() ?: 0
    }

    private fun stripExtension(name: String): String {
        if (name.isBlank()) return ""
        val dot = name.lastIndexOf('.')
        return if (dot > 0) name.substring(0, dot) else name
    }

    private companion object {
        const val UNKNOWN_ARTIST_TAG = "<unknown>"
    }
}
