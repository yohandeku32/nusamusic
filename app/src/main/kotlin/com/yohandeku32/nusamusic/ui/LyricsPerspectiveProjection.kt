package com.yohandeku32.nusamusic.ui

import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.Matrix

/**
 * A true 3D-to-2D perspective projection for a measured lyric grapheme or run.
 *
 * Compose scale/rotate only produce an affine 2D transform; enlarging a word
 * by 11% never gives it depth. Android Camera creates a perspective 3x3 matrix
 * (including foreshortening) for a subtle ribbon surface facing the listener.
 *
 * Each syllable reuses one Camera and Matrix for all of its characters.
 * Characters are measured once; drawing needs no bitmap or clipping strips.
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
        // The travelling surface supplies each character's local depth/tangent.
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
