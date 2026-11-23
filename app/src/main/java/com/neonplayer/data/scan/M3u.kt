package com.neonplayer.data.scan

import android.net.Uri
import com.neonplayer.domain.model.Track

object M3u {

    fun parse(text: String): List<String> = text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .toList()

    fun write(tracks: List<Track>): String = buildString {
        append("#EXTM3U\n")
        for (track in tracks) {
            append("#EXTINF:")
            append(track.durationMs / 1000L)
            append(',')
            append(track.displayArtist)
            append(" - ")
            append(track.title)
            append('\n')
            append(entryFor(track))
            append('\n')
        }
    }

    private fun entryFor(track: Track): String {
        val uri = track.uri
        if (!uri.startsWith("file://")) return uri
        return runCatching { Uri.parse(uri).path }.getOrNull() ?: uri
    }
}
