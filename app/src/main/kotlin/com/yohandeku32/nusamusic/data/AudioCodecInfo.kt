package com.yohandeku32.nusamusic.data

import java.util.Locale

data class AudioCodecInfo(
    val codecName: String,
    val sampleRateHz: Int?,
    val bitDepth: Int?
) {
    val isHiRes: Boolean
        get() = bitDepth != null && bitDepth >= 24

    val isDolby: Boolean
        get() = codecName.startsWith("Dolby ")
}

internal fun audioCodecName(mimeType: String): String {
    val mime = mimeType.lowercase(Locale.ROOT)
    return when {
        // Generic AC-3/E-AC-3/TrueHD streams do not establish the presence of Atmos.
        mime.contains("atmos") || (mime.contains("eac3") && mime.contains("joc")) -> "Dolby Atmos"
        mime.contains("eac3") -> "Dolby Digital+"
        mime.contains("ac3") -> "Dolby Digital"
        mime.contains("true-hd") || mime.contains("truehd") || mime == "audio/vnd.dolby.mlp" -> "Dolby TrueHD"
        mime.contains("ac4") -> "Dolby AC-4"
        mime.contains("flac") -> "FLAC"
        mime.contains("alac") -> "Apple Lossless"
        mime.contains("mp4a") || mime.contains("aac") -> "AAC"
        mime.contains("mpeg") -> "MP3"
        mime.contains("opus") -> "Opus"
        mime.contains("vorbis") -> "Vorbis"
        mime.contains("pcm") || mime.contains("raw") -> "PCM"
        mime.contains("dts") -> "DTS"
        mime.contains("amr") -> "AMR"
        else -> mime.removePrefix("audio/").uppercase(Locale.ROOT)
    }
}
