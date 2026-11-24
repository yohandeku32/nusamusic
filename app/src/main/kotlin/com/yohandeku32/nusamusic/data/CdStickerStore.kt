package com.yohandeku32.nusamusic.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import kotlin.math.roundToInt

internal object CdStickerStore {
    private const val MAX_EDGE_PX = 512

    private fun imageFile(context: Context) =
        AtomicFile(File(context.filesDir, "cd-sticker.png"))

    @Synchronized
    fun read(context: Context): Bitmap? = try {
        imageFile(context).openRead().use { BitmapFactory.decodeStream(it) }
    } catch (_: FileNotFoundException) {
        null
    }

    @Synchronized
    fun install(context: Context, uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val width = info.size.width
            val height = info.size.height
            val longestEdge = maxOf(width, height)
            if (longestEdge > MAX_EDGE_PX) {
                val scale = MAX_EDGE_PX.toFloat() / longestEdge
                decoder.setTargetSize(
                    (width * scale).roundToInt().coerceAtLeast(1),
                    (height * scale).roundToInt().coerceAtLeast(1)
                )
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }

        val file = imageFile(context)
        val output = file.startWrite()
        try {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            file.finishWrite(output)
        } catch (failure: Exception) {
            file.failWrite(output)
            throw failure
        }
        return bitmap
    }

    @Synchronized
    fun reset(context: Context) {
        val file = imageFile(context)
        file.delete()
        check(!file.baseFile.exists())
    }
}
