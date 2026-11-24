package com.neonplayer

import android.content.Context
import com.neonplayer.data.db.NeonDatabase
import com.neonplayer.data.repo.LibraryRepository
import com.neonplayer.data.repo.PlaylistRepository
import com.neonplayer.data.repo.QueueRepository
import com.neonplayer.data.scan.MediaStoreScanner
import com.neonplayer.data.scan.SafImporter
import com.neonplayer.data.scan.TagReader
import com.neonplayer.data.settings.SettingsRepository
import com.neonplayer.playback.EqualizerManager
import com.neonplayer.playback.PlayerConnection
import com.neonplayer.playback.SleepTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val context: Context = appContext

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val database: NeonDatabase = NeonDatabase.build(appContext)

    val settingsRepository = SettingsRepository(appContext, appScope)

    private val scanner = MediaStoreScanner(appContext)

    private val tagReader = TagReader(appContext)

    private val safImporter = SafImporter(appContext, tagReader)

    val libraryRepository = LibraryRepository(appContext, database, scanner, safImporter, appScope)

    val playlistRepository = PlaylistRepository(appContext, database, libraryRepository)

    val queueRepository = QueueRepository(database)

    val equalizerManager = EqualizerManager(appContext, settingsRepository, appScope)

    val playerConnection = PlayerConnection(appContext, libraryRepository, appScope)

    val sleepTimer = SleepTimer(playerConnection, appScope)

    val playbackEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
}
