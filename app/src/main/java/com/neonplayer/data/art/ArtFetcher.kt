package com.neonplayer.data.art

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.FileSystem
import java.io.ByteArrayOutputStream
import java.io.IOException

data class ArtModel(val trackUri: String)

class ArtFetcher(
    private val context: Context,
    private val data: ArtModel,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val bytes = loadBytes() ?: throw IOException("No embedded artwork for ${data.trackUri}")
        SourceFetchResult(
            source = ImageSource(Buffer().write(bytes), FileSystem.SYSTEM),
            mimeType = null,
            dataSource = DataSource.DISK,
        )
    }

    private fun loadBytes(): ByteArray? {
        val uri = runCatching { Uri.parse(data.trackUri) }.getOrNull() ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val thumbnail = runCatching {
                context.contentResolver.loadThumbnail(uri, Size(THUMB_SIZE, THUMB_SIZE), null)
            }.getOrNull()
            if (thumbnail != null) {
                val out = ByteArrayOutputStream()
                if (thumbnail.compress(Bitmap.CompressFormat.PNG, 100, out)) return out.toByteArray()
            }
        }
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.embeddedPicture
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<ArtModel> {
        override fun create(data: ArtModel, options: Options, imageLoader: ImageLoader): Fetcher =
            ArtFetcher(context, data)
    }

    private companion object {
        const val THUMB_SIZE = 512
    }
}

class ArtKeyer : Keyer<ArtModel> {
    override fun key(data: ArtModel, options: Options): String = data.trackUri
}
