package com.neonplayer.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistEntryEntity::class,
        QueueItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class NeonDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao

    abstract fun playlistDao(): PlaylistDao

    abstract fun queueDao(): QueueDao

    companion object {
        const val NAME = "neon.db"

        fun build(context: Context): NeonDatabase = Room.databaseBuilder(
            context.applicationContext,
            NeonDatabase::class.java,
            NAME,
        ).build()
    }
}
