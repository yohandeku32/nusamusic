package com.yohandeku32.nusamusic.data

import java.io.InputStream

/** Read native FLAC STREAMINFO without depending on Android's decoder metadata. */
internal fun readFlacCodecInfo(input: InputStream): AudioCodecInfo? {
    fun readBytes(count: Int): ByteArray? {
        val bytes = ByteArray(count)
        var offset = 0
        while (offset < count) {
            val read = input.read(bytes, offset, count - offset)
            if (read < 0) return null
            if (read == 0) {
                val byte = input.read()
                if (byte < 0) return null
                bytes[offset++] = byte.toByte()
            } else offset += read
        }
        return bytes
    }
    var marker = readBytes(4) ?: return null
    // Some tagged files prepend ID3v2. Bound the skip and never scan audio data.
    if (marker.take(3) == listOf(73.toByte(), 68.toByte(), 51.toByte())) {
        val rest = readBytes(6) ?: return null
        if ((2..5).any { rest[it].toInt() and 128 != 0 }) return null
        var tagSize = 0L
        for (index in 2..5) tagSize = (tagSize shl 7) or rest[index].toLong()
        if (tagSize > 16 * 1024 * 1024) return null
        if (marker[3].toInt() == 4 && rest[1].toInt() and 16 != 0) tagSize += 10
        while (tagSize > 0) {
            val skipped = input.skip(tagSize)
            if (skipped > 0) tagSize -= skipped else {
                if (input.read() < 0) return null
                tagSize--
            }
        }
        marker = readBytes(4) ?: return null
    }
    if (!marker.contentEquals(byteArrayOf(102, 76, 97, 67))) return null
    val header = readBytes(4) ?: return null
    val length = ((header[1].toInt() and 255) shl 16) or
        ((header[2].toInt() and 255) shl 8) or (header[3].toInt() and 255)
    if (header[0].toInt() and 127 != 0 || length != 34) return null
    val info = readBytes(34) ?: return null
    fun byte(index: Int) = info[index].toInt() and 255
    val rate = (byte(10) shl 12) or (byte(11) shl 4) or (byte(12) ushr 4)
    val depth = (((byte(12) and 1) shl 4) or (byte(13) ushr 4)) + 1
    if (rate !in 1..655350 || depth !in 4..32) return null
    return AudioCodecInfo("FLAC", rate, depth)
}
