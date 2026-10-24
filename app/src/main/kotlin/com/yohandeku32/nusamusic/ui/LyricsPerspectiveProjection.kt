package com.yohandeku32.nusamusic.ui

import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.Matrix

/**
 * A true 3D-to-2D perspective projection for an intact shaped lyric run.
 *
 * Compose scale/rotate only produce an affine 2D transform; enlarging a word
 * by 11% never gives it depth. Android Camera creates a perspective 3x3 matrix
 * (including foreshortening) for a subtle ribbon surface facing the listener.
 *
 * Each measured syllable owns one reusable Camera and Matrix. No bitmap
 * allocation, per-character slicing, animation coroutine or extra timing
 * clock is needed, even at 120 Hz.
 */
internal class LyricsPerspectiveProjection {
    private val camera = Camera().apply {
        // Further camera distance prevents exaggerated foreshortening that
        // scrambles a word's spacing, while retaining subtle 3D curvature.
        setLocation(0f, 0f, -14f)
    }
    private val matrix = Matrix()

    fun apply(
        canvas: Canvas,
        pivotX: Float,
        pivotY: Float,
        depth: Float,
        slope: Float
    ) {
        val amount = depth.coerceIn(0f, 1f)
        if (amount <= 0.0001f) return

        camera.save()
        // Gentle 3D angles only: the upward arc carries the visible motion.
        // The old 11/17-degree tilts looked like separate letters twisting
        // forward at different sizes rather than one coherent sung word.
        camera.rotateX(4f * amount)
        camera.rotateY((-4.5f * slope.coerceIn(-1f, 1f)) * amount)
        camera.getMatrix(matrix)
        camera.restore()

        // Camera's perspective is around the local origin. Translate to the
        // shaped word's baseline pivot and back without moving its layout.
        matrix.preTranslate(-pivotX, -pivotY)
        matrix.postTranslate(pivotX, pivotY)
        canvas.concat(matrix)
    }
}
