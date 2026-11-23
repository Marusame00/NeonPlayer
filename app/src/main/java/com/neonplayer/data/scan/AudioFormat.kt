package com.neonplayer.data.scan

import java.util.Locale

object AudioFormat {

    val SUPPORTED_EXTENSIONS = setOf("mp3", "flac", "wav", "ogg", "m4a", "aac", "opus")

    private val BY_EXTENSION = mapOf(
        "mp3" to "MP3",
        "flac" to "FLAC",
        "wav" to "WAV",
        "wave" to "WAV",
        "ogg" to "OGG",
        "oga" to "OGG",
        "opus" to "OPUS",
        "m4a" to "M4A",
        "mp4" to "M4A",
        "aac" to "AAC",
    )

    private val BY_MIME = mapOf(
        "audio/mpeg" to "MP3",
        "audio/mp3" to "MP3",
        "audio/x-mpeg" to "MP3",
        "audio/flac" to "FLAC",
        "audio/x-flac" to "FLAC",
        "audio/wav" to "WAV",
        "audio/x-wav" to "WAV",
        "audio/wave" to "WAV",
        "audio/vnd.wave" to "WAV",
        "audio/ogg" to "OGG",
        "application/ogg" to "OGG",
        "audio/opus" to "OPUS",
        "audio/mp4" to "M4A",
        "audio/m4a" to "M4A",
        "audio/x-m4a" to "M4A",
        "audio/aac" to "AAC",
        "audio/aac-adts" to "AAC",
    )

    fun extensionOf(name: String?): String {
        if (name.isNullOrBlank()) return ""
        val dot = name.lastIndexOf('.')
        if (dot < 0 || dot == name.length - 1) return ""
        return name.substring(dot + 1).lowercase(Locale.ROOT)
    }

    fun isSupported(name: String?): Boolean = extensionOf(name) in SUPPORTED_EXTENSIONS

    fun of(displayName: String?, mimeType: String?): String {
        val ext = extensionOf(displayName)
        BY_EXTENSION[ext]?.let { return it }
        val mime = mimeType?.lowercase(Locale.ROOT)?.substringBefore(';')?.trim()
        if (!mime.isNullOrEmpty()) BY_MIME[mime]?.let { return it }
        return ext.uppercase(Locale.ROOT)
    }
}
