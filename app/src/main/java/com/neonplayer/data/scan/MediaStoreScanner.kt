package com.neonplayer.data.scan

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.neonplayer.domain.model.TrackSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreScanner(private val context: Context) {

    suspend fun scan(): List<ScannedTrack> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ARTIST)
            add(MediaStore.Audio.Media.TRACK)
            add(MediaStore.Audio.Media.YEAR)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.MIME_TYPE)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.IS_MUSIC)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                add(MediaStore.Audio.Media.DATA)
            }
        }.toTypedArray()

        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val args = arrayOf(MIN_DURATION_MS.toString())
        val out = ArrayList<ScannedTrack>()

        context.contentResolver.query(collection, projection, selection, args, null)?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumArtistIdx = cursor.getColumnIndexOrThrow("album_artist")
            val trackIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val durationIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val mimeIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dateIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val isMusicIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC)
            val folderIdx = cursor.getColumnIndexOrThrow(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Audio.Media.RELATIVE_PATH
                } else {
                    MediaStore.Audio.Media.DATA
                },
            )

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val name = cursor.getString(nameIdx)
                val mime = cursor.getString(mimeIdx).orEmpty()
                val isMusic = cursor.getInt(isMusicIdx) != 0
                val supported = AudioFormat.isSupported(name)
                if (!isMusic && !(mime.startsWith("audio/") && supported)) continue

                val rawTitle = cursor.getString(titleIdx)
                val title = if (rawTitle.isNullOrBlank()) stripExtension(name) else rawTitle
                val artist = cursor.getString(artistIdx).orEmpty().let { if (it == UNKNOWN_ARTIST_TAG) "" else it }
                val album = cursor.getString(albumIdx).orEmpty()
                val albumArtist = cursor.getString(albumArtistIdx).orEmpty()
                val rawTrack = cursor.getInt(trackIdx)
                val discNumber = rawTrack / 1000
                val trackNumber = rawTrack % 1000
                val year = cursor.getInt(yearIdx)
                val duration = cursor.getLong(durationIdx)
                val dateAdded = cursor.getLong(dateIdx) * 1000L
                val size = cursor.getLong(sizeIdx)
                val folder = folderOf(cursor.getString(folderIdx))

                out.add(
                    ScannedTrack(
                        uri = ContentUris.withAppendedId(collection, id).toString(),
                        title = title,
                        artist = artist,
                        album = album,
                        albumArtist = albumArtist,
                        trackNumber = trackNumber,
                        discNumber = discNumber,
                        year = year,
                        durationMs = duration,
                        format = AudioFormat.of(name, mime),
                        mimeType = mime,
                        folder = folder,
                        dateAdded = dateAdded,
                        sizeBytes = size,
                        source = TrackSource.MEDIASTORE,
                    ),
                )
            }
        }
        out
    }

    private fun folderOf(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            raw.trimEnd('/')
        } else {
            File(raw).parent.orEmpty()
        }
    }

    private fun stripExtension(name: String?): String {
        if (name.isNullOrBlank()) return ""
        val dot = name.lastIndexOf('.')
        return if (dot > 0) name.substring(0, dot) else name
    }

    private companion object {
        const val MIN_DURATION_MS = 5_000L
        const val UNKNOWN_ARTIST_TAG = "<unknown>"
    }
}
