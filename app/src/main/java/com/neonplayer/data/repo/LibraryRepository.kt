package com.neonplayer.data.repo

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.neonplayer.R
import com.neonplayer.data.db.NeonDatabase
import com.neonplayer.data.db.TrackEntity
import com.neonplayer.data.db.toTrack
import com.neonplayer.data.scan.MediaStoreScanner
import com.neonplayer.data.scan.SafImporter
import com.neonplayer.data.scan.ScannedTrack
import com.neonplayer.data.scan.toEntity
import com.neonplayer.domain.model.AlbumGroup
import com.neonplayer.domain.model.ArtistGroup
import com.neonplayer.domain.model.FolderGroup
import com.neonplayer.domain.model.ScanState
import com.neonplayer.domain.model.SortOrder
import com.neonplayer.domain.model.Track
import com.neonplayer.domain.model.TrackSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale

class LibraryRepository(
    private val context: Context,
    private val db: NeonDatabase,
    private val scanner: MediaStoreScanner,
    private val safImporter: SafImporter,
    private val scope: CoroutineScope,
) {

    private val trackDao = db.trackDao()
    private val rescanMutex = Mutex()
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)

    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    fun tracks(sort: SortOrder, query: String): Flow<List<Track>> {
        val q = query.trim()
        val source = when (sort) {
            SortOrder.TITLE -> trackDao.observeByTitle(q)
            SortOrder.ARTIST -> trackDao.observeByArtist(q)
            SortOrder.ALBUM -> trackDao.observeByAlbum(q)
            SortOrder.DATE_ADDED -> trackDao.observeByDateAdded(q)
            SortOrder.DURATION -> trackDao.observeByDuration(q)
            SortOrder.MANUAL -> trackDao.observeByManual(q)
        }
        return source.map { list -> list.map(TrackEntity::toTrack) }.flowOn(Dispatchers.Default)
    }

    fun albums(query: String): Flow<List<AlbumGroup>> = tracks(SortOrder.TITLE, query)
        .map { list ->
            list.groupBy { it.displayAlbum.lowercase(Locale.ROOT) }
                .flatMap { (_, sameAlbum) -> splitByAlbumArtist(sameAlbum) }
                .map { group ->
                    val sorted = group.sortedWith(compareBy({ it.discNumber }, { it.trackNumber }, { it.title.lowercase(Locale.ROOT) }))
                    AlbumGroup(
                        key = "${sorted.first().displayAlbum}|${sorted.first().albumArtist.trim().lowercase(Locale.ROOT)}",
                        title = sorted.first().displayAlbum,
                        artist = sorted.albumArtistLabel(),
                        year = sorted.firstOrNull { it.year > 0 }?.year ?: 0,
                        tracks = sorted,
                    )
                }
                .sortedBy { it.title.lowercase(Locale.ROOT) }
        }
        .flowOn(Dispatchers.Default)

    /**
     * Album name wins: tracks that share an album name stay together even when their
     * per-track artists differ, which is what a multi-artist album looks like when the
     * albumArtist tag is missing. Only a genuinely different albumArtist splits a group,
     * so two distinct albums that happen to share a name are still kept apart.
     */
    private fun splitByAlbumArtist(tracks: List<Track>): List<List<Track>> {
        val distinct = tracks.map { it.albumArtist.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }
            .distinct()
        if (distinct.size <= 1) return listOf(tracks)
        return tracks.groupBy { it.albumArtist.trim().lowercase(Locale.ROOT) }.values.toList()
    }

    private fun List<Track>.albumArtistLabel(): String {
        firstOrNull { it.albumArtist.isNotBlank() }?.let { return it.albumArtist }
        val artists = map { it.displayArtist }.distinct()
        return if (artists.size == 1) artists.first() else context.getString(R.string.album_various_artists)
    }

    fun artists(query: String): Flow<List<ArtistGroup>> = tracks(SortOrder.TITLE, query)
        .map { list ->
            list.groupBy { it.groupArtist }
                .map { (name, group) ->
                    ArtistGroup(
                        name = name,
                        tracks = group.sortedWith(
                            compareBy(
                                { it.album.lowercase(Locale.ROOT) },
                                { it.discNumber },
                                { it.trackNumber },
                                { it.title.lowercase(Locale.ROOT) },
                            ),
                        ),
                    )
                }
                .sortedBy { it.name.lowercase(Locale.ROOT) }
        }
        .flowOn(Dispatchers.Default)

    fun folders(query: String): Flow<List<FolderGroup>> = tracks(SortOrder.TITLE, query)
        .map { list ->
            list.filter { it.folder.isNotBlank() }
                .groupBy { it.folder }
                .map { (path, group) ->
                    FolderGroup(
                        path = path,
                        name = path.substringAfterLast('/').substringAfterLast('\\'),
                        tracks = group.sortedWith(
                            compareBy(
                                { it.discNumber },
                                { it.trackNumber },
                                { it.title.lowercase(Locale.ROOT) },
                            ),
                        ),
                    )
                }
                .sortedBy { it.name.lowercase(Locale.ROOT) }
        }
        .flowOn(Dispatchers.Default)

    fun trackCount(): Flow<Int> = trackDao.observeCount().flowOn(Dispatchers.Default)

    suspend fun getTracks(ids: List<Long>): List<Track> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()
        val byId = HashMap<Long, Track>(ids.size)
        for (chunk in ids.chunked(SQLITE_ID_CHUNK)) {
            for (entity in trackDao.getByIds(chunk)) byId[entity.id] = entity.toTrack()
        }
        ids.mapNotNull { byId[it] }
    }

    suspend fun getTrack(id: Long): Track? = withContext(Dispatchers.IO) {
        trackDao.getById(id)?.toTrack()
    }

    suspend fun findByUriOrPath(entry: String): Track? = withContext(Dispatchers.IO) {
        val value = entry.trim()
        if (value.isEmpty()) return@withContext null
        trackDao.findByUri(value)?.let { return@withContext it.toTrack() }
        trackDao.findByUriSuffix(value)?.let { return@withContext it.toTrack() }
        val name = value.substringAfterLast('/').substringAfterLast('\\')
        if (name.isNotEmpty() && name != value) {
            trackDao.findByUriSuffix("/$name")?.let { return@withContext it.toTrack() }
        }
        null
    }

    fun rescan() {
        scope.launch {
            rescanMutex.withLock {
                _scanState.value = ScanState.Scanning(0, 0)
                try {
                    val scanned = scanner.scan()
                    _scanState.value = ScanState.Scanning(scanned.size, scanned.size)
                    val existing = trackDao.allOnce().associateBy { it.uri }
                    var order = trackDao.maxManualOrder()
                    val inserts = ArrayList<TrackEntity>()
                    val updates = ArrayList<TrackEntity>()
                    for (item in scanned) {
                        val old = existing[item.uri]
                        if (old == null) {
                            order += 1
                            inserts.add(item.toEntity(0L, order))
                        } else {
                            updates.add(item.toEntity(old.id, old.manualOrder, old.dateAdded))
                        }
                    }
                    val present = scanned.mapTo(HashSet(scanned.size)) { it.uri }
                    val removed = existing.values.filter {
                        it.source == TrackSource.MEDIASTORE.name && it.uri !in present
                    }
                    db.withTransaction {
                        if (inserts.isNotEmpty()) trackDao.insertAll(inserts)
                        if (updates.isNotEmpty()) trackDao.updateAll(updates)
                        if (removed.isNotEmpty()) trackDao.deleteByIds(removed.map { it.id })
                    }
                    _scanState.value = ScanState.Done(inserts.size, removed.size, scanned.size)
                } catch (t: Throwable) {
                    _scanState.value = ScanState.Failed(t.message ?: "Scan failed")
                }
            }
        }
    }

    suspend fun importFiles(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        insertScanned(safImporter.importFiles(uris))
    }

    suspend fun importFolder(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        insertScanned(safImporter.importTree(treeUri))
    }

    suspend fun removeTracks(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        for (chunk in ids.chunked(SQLITE_ID_CHUNK)) trackDao.deleteByIds(chunk)
    }

    suspend fun setManualOrder(orderedIds: List<Long>) = withContext(Dispatchers.IO) {
        db.withTransaction {
            orderedIds.forEachIndexed { index, id -> trackDao.updateManualOrder(id, index) }
        }
    }

    suspend fun markMissing(id: Long) = withContext(Dispatchers.IO) {
        trackDao.markMissing(id)
    }

    private suspend fun insertScanned(scanned: List<ScannedTrack>): Int {
        if (scanned.isEmpty()) return 0
        val known = trackDao.allOnce().mapTo(HashSet()) { it.uri }
        var order = trackDao.maxManualOrder()
        val fresh = ArrayList<TrackEntity>(scanned.size)
        for (item in scanned) {
            if (!known.add(item.uri)) continue
            order += 1
            fresh.add(item.toEntity(0L, order))
        }
        if (fresh.isNotEmpty()) trackDao.insertAll(fresh)
        return fresh.size
    }

    private companion object {
        const val SQLITE_ID_CHUNK = 900
    }
}
