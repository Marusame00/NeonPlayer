package com.neonplayer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class PlaylistRow(
    val id: Long,
    val name: String,
    val position: Int,
    val createdAt: Long,
    val trackCount: Int,
    val totalDurationMs: Long,
)

@Dao
interface PlaylistDao {

    @Query(
        "SELECT p.id AS id, p.name AS name, p.position AS position, p.createdAt AS createdAt, " +
            "COUNT(e.id) AS trackCount, COALESCE(SUM(t.durationMs), 0) AS totalDurationMs " +
            "FROM playlists p LEFT JOIN playlist_entries e ON e.playlistId = p.id " +
            "LEFT JOIN tracks t ON t.id = e.trackId GROUP BY p.id ORDER BY p.position ASC",
    )
    fun observePlaylists(): Flow<List<PlaylistRow>>

    @Query(
        "SELECT t.* FROM tracks t INNER JOIN playlist_entries e ON e.trackId = t.id " +
            "WHERE e.playlistId = :playlistId ORDER BY e.position ASC",
    )
    fun observeTracks(playlistId: Long): Flow<List<TrackEntity>>

    @Query(
        "SELECT t.* FROM tracks t INNER JOIN playlist_entries e ON e.trackId = t.id " +
            "WHERE e.playlistId = :playlistId ORDER BY e.position ASC",
    )
    suspend fun tracksOnce(playlistId: Long): List<TrackEntity>

    @Query("SELECT * FROM playlists ORDER BY position ASC")
    suspend fun playlistsOnce(): List<PlaylistEntity>

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlists")
    suspend fun maxPosition(): Int

    @Insert
    suspend fun insert(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE playlists SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)

    @Insert
    suspend fun insertEntries(entries: List<PlaylistEntryEntity>)

    @Query("DELETE FROM playlist_entries WHERE playlistId = :playlistId")
    suspend fun deleteEntries(playlistId: Long)

    @Query("SELECT * FROM playlist_entries WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun entries(playlistId: Long): List<PlaylistEntryEntity>

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_entries WHERE playlistId = :playlistId")
    suspend fun maxEntryPosition(playlistId: Long): Int

    @Query("DELETE FROM playlist_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Query("UPDATE playlist_entries SET position = :position WHERE id = :id")
    suspend fun updateEntryPosition(id: Long, position: Int)
}
