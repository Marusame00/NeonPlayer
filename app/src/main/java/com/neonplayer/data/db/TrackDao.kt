package com.neonplayer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Query(
        "SELECT * FROM tracks WHERE (:q = '' OR title LIKE '%' || :q || '%' OR artist LIKE '%' || :q || '%' " +
            "OR album LIKE '%' || :q || '%') ORDER BY titleSort ASC",
    )
    fun observeByTitle(q: String): Flow<List<TrackEntity>>

    @Query(
        "SELECT * FROM tracks WHERE (:q = '' OR title LIKE '%' || :q || '%' OR artist LIKE '%' || :q || '%' " +
            "OR album LIKE '%' || :q || '%') ORDER BY artist COLLATE NOCASE ASC, album ASC, discNumber ASC, trackNumber ASC",
    )
    fun observeByArtist(q: String): Flow<List<TrackEntity>>

    @Query(
        "SELECT * FROM tracks WHERE (:q = '' OR title LIKE '%' || :q || '%' OR artist LIKE '%' || :q || '%' " +
            "OR album LIKE '%' || :q || '%') ORDER BY album COLLATE NOCASE ASC, discNumber ASC, trackNumber ASC",
    )
    fun observeByAlbum(q: String): Flow<List<TrackEntity>>

    @Query(
        "SELECT * FROM tracks WHERE (:q = '' OR title LIKE '%' || :q || '%' OR artist LIKE '%' || :q || '%' " +
            "OR album LIKE '%' || :q || '%') ORDER BY dateAdded DESC",
    )
    fun observeByDateAdded(q: String): Flow<List<TrackEntity>>

    @Query(
        "SELECT * FROM tracks WHERE (:q = '' OR title LIKE '%' || :q || '%' OR artist LIKE '%' || :q || '%' " +
            "OR album LIKE '%' || :q || '%') ORDER BY durationMs DESC",
    )
    fun observeByDuration(q: String): Flow<List<TrackEntity>>

    @Query(
        "SELECT * FROM tracks WHERE (:q = '' OR title LIKE '%' || :q || '%' OR artist LIKE '%' || :q || '%' " +
            "OR album LIKE '%' || :q || '%') ORDER BY manualOrder ASC",
    )
    fun observeByManual(q: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY titleSort ASC")
    fun observeAll(): Flow<List<TrackEntity>>

    @Query("SELECT COUNT(*) FROM tracks")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM tracks")
    suspend fun allOnce(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getById(id: Long): TrackEntity?

    @Query("SELECT * FROM tracks WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE uri = :uri LIMIT 1")
    suspend fun findByUri(uri: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE uri LIKE '%' || :suffix LIMIT 1")
    suspend fun findByUriSuffix(suffix: String): TrackEntity?

    @Query("SELECT COALESCE(MAX(manualOrder), 0) FROM tracks")
    suspend fun maxManualOrder(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: TrackEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<TrackEntity>): List<Long>

    @Update
    suspend fun updateAll(entities: List<TrackEntity>)

    @Query("DELETE FROM tracks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE tracks SET manualOrder = :order WHERE id = :id")
    suspend fun updateManualOrder(id: Long, order: Int)

    @Query("UPDATE tracks SET missing = 1 WHERE id = :id")
    suspend fun markMissing(id: Long)
}
