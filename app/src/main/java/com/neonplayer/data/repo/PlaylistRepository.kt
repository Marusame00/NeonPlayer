package com.neonplayer.data.repo

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.withTransaction
import com.neonplayer.data.db.NeonDatabase
import com.neonplayer.data.db.PlaylistEntity
import com.neonplayer.data.db.PlaylistEntryEntity
import com.neonplayer.data.db.toTrack
import com.neonplayer.data.scan.M3u
import com.neonplayer.domain.model.Playlist
import com.neonplayer.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class PlaylistRepository(
    private val context: Context,
    private val db: NeonDatabase,
    private val library: LibraryRepository,
) {

    private val playlistDao = db.playlistDao()

    fun playlists(): Flow<List<Playlist>> = playlistDao.observePlaylists()
        .map { rows ->
            rows.map {
                Playlist(
                    id = it.id,
                    name = it.name,
                    trackCount = it.trackCount,
                    totalDurationMs = it.totalDurationMs,
                    position = it.position,
                    createdAt = it.createdAt,
                )
            }
        }
        .flowOn(Dispatchers.Default)

    fun playlistTracks(playlistId: Long): Flow<List<Track>> = playlistDao.observeTracks(playlistId)
        .map { list -> list.map { it.toTrack() } }
        .flowOn(Dispatchers.Default)

    suspend fun create(name: String): Long = withContext(Dispatchers.IO) {
        val position = playlistDao.maxPosition() + 1
        playlistDao.insert(
            PlaylistEntity(
                name = name.trim().ifEmpty { "Playlist" },
                position = position,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun rename(id: Long, name: String) = withContext(Dispatchers.IO) {
        playlistDao.rename(id, name.trim().ifEmpty { "Playlist" })
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        playlistDao.delete(id)
    }

    suspend fun setPlaylistOrder(orderedIds: List<Long>) = withContext(Dispatchers.IO) {
        db.withTransaction {
            orderedIds.forEachIndexed { index, id -> playlistDao.updatePosition(id, index) }
        }
    }

    suspend fun addTracks(playlistId: Long, trackIds: List<Long>) = withContext(Dispatchers.IO) {
        if (trackIds.isEmpty()) return@withContext
        db.withTransaction {
            var position = playlistDao.maxEntryPosition(playlistId) + 1
            val entries = trackIds.map { trackId ->
                PlaylistEntryEntity(playlistId = playlistId, trackId = trackId, position = position++)
            }
            playlistDao.insertEntries(entries)
        }
    }

    suspend fun removeAt(playlistId: Long, index: Int) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val entries = playlistDao.entries(playlistId)
            val target = entries.getOrNull(index) ?: return@withTransaction
            playlistDao.deleteEntry(target.id)
            entries.filter { it.id != target.id }.forEachIndexed { newPosition, entry ->
                if (entry.position != newPosition) playlistDao.updateEntryPosition(entry.id, newPosition)
            }
        }
    }

    suspend fun reorder(playlistId: Long, orderedTrackIds: List<Long>) = withContext(Dispatchers.IO) {
        db.withTransaction {
            playlistDao.deleteEntries(playlistId)
            if (orderedTrackIds.isNotEmpty()) {
                playlistDao.insertEntries(
                    orderedTrackIds.mapIndexed { index, trackId ->
                        PlaylistEntryEntity(playlistId = playlistId, trackId = trackId, position = index)
                    },
                )
            }
        }
    }

    suspend fun importM3u(uri: Uri): Pair<Long, Int> = withContext(Dispatchers.IO) {
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        }.getOrNull() ?: return@withContext 0L to 0
        val name = displayName(uri)?.substringBeforeLast('.')?.trim().orEmpty().ifEmpty { "Imported playlist" }
        val matched = ArrayList<Long>()
        for (entry in M3u.parse(text)) {
            library.findByUriOrPath(entry)?.let { matched.add(it.id) }
        }
        val playlistId = create(name)
        if (matched.isNotEmpty()) addTracks(playlistId, matched)
        playlistId to matched.size
    }

    suspend fun exportM3u(playlistId: Long, dest: Uri) = withContext(Dispatchers.IO) {
        val tracks = playlistDao.tracksOnce(playlistId).map { it.toTrack() }
        val text = M3u.write(tracks)
        context.contentResolver.openOutputStream(dest)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment
}
