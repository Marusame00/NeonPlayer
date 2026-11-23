package com.neonplayer

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.neonplayer.data.art.ArtFetcher
import com.neonplayer.data.art.ArtKeyer
import okio.Path.Companion.toOkioPath

class NeonApp : Application(), SingletonImageLoader.Factory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun newImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(ArtFetcher.Factory(context))
            add(ArtKeyer())
            add(VideoFrameDecoder.Factory())
        }
        .memoryCache {
            MemoryCache.Builder().maxSizePercent(context, MEMORY_CACHE_PERCENT).build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("art").toOkioPath())
                .maxSizeBytes(DISK_CACHE_BYTES)
                .build()
        }
        .crossfade(CROSSFADE_MS)
        .build()

    private companion object {
        const val MEMORY_CACHE_PERCENT = 0.2
        const val DISK_CACHE_BYTES = 100L * 1024L * 1024L
        const val CROSSFADE_MS = 200
    }
}
