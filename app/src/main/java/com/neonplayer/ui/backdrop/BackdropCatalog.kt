package com.neonplayer.ui.backdrop

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.neonplayer.domain.model.BACKDROP_ASSET_PREFIX
import com.neonplayer.domain.model.BACKDROP_CUSTOM_PREFIX
import com.neonplayer.domain.model.BACKDROP_STATIC
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

enum class BackdropKind { STATIC, VIDEO }

data class BackdropOption(
    val id: String,
    val label: String,
    val kind: BackdropKind,
    val playUri: String?,
    val thumbUri: String?,
    val assetFile: String? = null,
)

object BackdropCatalog {

    private const val ASSET_DIR = "backdrops"
    private val VIDEO_EXTENSIONS = listOf("mp4", "webm", "mkv")

    @Volatile
    private var assetCache: List<String>? = null

    private val posterCache = ConcurrentHashMap<String, String>()

    fun list(context: Context, custom: List<String>): List<BackdropOption> {
        val options = ArrayList<BackdropOption>(custom.size + 4)
        options.add(BackdropOption(BACKDROP_STATIC, "Static synthwave", BackdropKind.STATIC, null, null))
        for (file in assetVideos(context)) {
            options.add(
                BackdropOption(
                    id = BACKDROP_ASSET_PREFIX + file,
                    label = labelFor(file),
                    kind = BackdropKind.VIDEO,
                    playUri = "asset:///$ASSET_DIR/$file",
                    thumbUri = "file:///android_asset/$ASSET_DIR/$file",
                    assetFile = file,
                ),
            )
        }
        custom.forEachIndexed { index, uri ->
            options.add(
                BackdropOption(
                    id = BACKDROP_CUSTOM_PREFIX + uri,
                    label = customLabel(context, uri) ?: "My video ${index + 1}",
                    kind = BackdropKind.VIDEO,
                    playUri = uri,
                    thumbUri = uri,
                ),
            )
        }
        return options
    }

    fun resolve(context: Context, id: String, custom: List<String>): BackdropOption {
        val options = list(context, custom)
        return options.firstOrNull { it.id == id } ?: options.first()
    }

    private fun assetVideos(context: Context): List<String> {
        assetCache?.let { return it }
        val files = runCatching {
            context.assets.list(ASSET_DIR)
                ?.filter { name -> VIDEO_EXTENSIONS.any { name.endsWith(".$it", ignoreCase = true) } }
                ?.sorted()
                .orEmpty()
        }.getOrDefault(emptyList())
        assetCache = files
        return files
    }

    fun posterFor(context: Context, assetFile: String): String? {
        posterCache[assetFile]?.let { return it }
        val out = File(
            context.cacheDir,
            "backdrop_poster_" + assetFile.substringBeforeLast('.').replace(Regex("[^A-Za-z0-9]"), "_") + ".jpg",
        )
        if (out.exists() && out.length() > 0L) {
            posterCache[assetFile] = out.absolutePath
            return out.absolutePath
        }
        val retriever = MediaMetadataRetriever()
        return try {
            context.assets.openFd("$ASSET_DIR/$assetFile").use { afd ->
                retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            val frame = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
                ?: return null
            FileOutputStream(out).use { stream ->
                frame.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            }
            frame.recycle()
            if (out.length() <= 0L) return null
            posterCache[assetFile] = out.absolutePath
            out.absolutePath
        } catch (t: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun labelFor(fileName: String): String {
        val base = fileName.substringBeforeLast('.')
        val stripped = base.dropWhile { it.isDigit() || it == '_' || it == '-' || it == '.' || it == ' ' }
        val cleaned = stripped.ifBlank { base }
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
        return cleaned.split(' ')
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
    }

    private fun customLabel(context: Context, uri: String): String? {
        val parsed = runCatching { Uri.parse(uri) }.getOrNull() ?: return null
        val documentName = runCatching { DocumentFile.fromSingleUri(context, parsed)?.name }.getOrNull()
        if (!documentName.isNullOrBlank()) return documentName
        val queried = runCatching {
            context.contentResolver.query(parsed, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        return queried?.takeIf { it.isNotBlank() }
    }
}
