package com.neonplayer.ui.backdrop

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.neonplayer.domain.model.BACKDROP_ASSET_PREFIX
import com.neonplayer.domain.model.BACKDROP_CUSTOM_PREFIX
import com.neonplayer.domain.model.BACKDROP_STATIC

enum class BackdropKind { STATIC, VIDEO }

data class BackdropOption(
    val id: String,
    val label: String,
    val kind: BackdropKind,
    val playUri: String?,
    val thumbUri: String?,
)

object BackdropCatalog {

    private const val ASSET_DIR = "backdrops"
    private val VIDEO_EXTENSIONS = listOf("mp4", "webm", "mkv")

    @Volatile
    private var assetCache: List<String>? = null

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
