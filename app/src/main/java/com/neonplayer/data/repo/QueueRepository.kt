package com.neonplayer.data.repo

import androidx.room.withTransaction
import com.neonplayer.data.db.NeonDatabase
import com.neonplayer.data.db.QueueItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QueueRepository(private val db: NeonDatabase) {

    private val queueDao = db.queueDao()

    suspend fun save(trackIds: List<Long>) = withContext(Dispatchers.IO) {
        db.withTransaction {
            queueDao.clear()
            if (trackIds.isNotEmpty()) {
                queueDao.insertAll(trackIds.mapIndexed { index, id -> QueueItemEntity(index, id) })
            }
        }
    }

    suspend fun load(): List<Long> = withContext(Dispatchers.IO) {
        queueDao.load().map { it.trackId }
    }
}
