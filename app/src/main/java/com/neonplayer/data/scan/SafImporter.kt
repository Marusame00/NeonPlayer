package com.neonplayer.data.scan

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SafImporter(
    private val context: Context,
    private val tagReader: TagReader,
) {

    suspend fun importFiles(uris: List<Uri>): List<ScannedTrack> = withContext(Dispatchers.IO) {
        val out = ArrayList<ScannedTrack>(uris.size)
        for (uri in uris) {
            persist(uri)
            val track = runCatching { tagReader.read(uri) }.getOrNull()
            if (track != null) out.add(track)
        }
        out
    }

    suspend fun importTree(treeUri: Uri): List<ScannedTrack> = withContext(Dispatchers.IO) {
        persist(treeUri)
        val root = runCatching { DocumentFile.fromTreeUri(context, treeUri) }.getOrNull()
            ?: return@withContext emptyList()
        val out = ArrayList<ScannedTrack>()
        walk(root, root.name.orEmpty(), out)
        out
    }

    private suspend fun walk(dir: DocumentFile, path: String, out: MutableList<ScannedTrack>) {
        val children = runCatching { dir.listFiles() }.getOrNull() ?: return
        for (child in children) {
            val name = child.name.orEmpty()
            if (child.isDirectory) {
                val childPath = if (path.isEmpty()) name else "$path/$name"
                walk(child, childPath, out)
            } else if (AudioFormat.isSupported(name)) {
                val track = runCatching { tagReader.read(child.uri, path) }.getOrNull()
                if (track != null) out.add(track)
            }
        }
    }

    private fun persist(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
        }
    }
}
