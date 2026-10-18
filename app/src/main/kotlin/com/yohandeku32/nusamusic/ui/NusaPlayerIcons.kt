package com.yohandeku32.nusamusic.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.dp

/**
 * Consistent, lightweight NusaMusic Now Playing icon family.
 *
 * Soft terminals, rounded joins, restrained 1.85dp stroke, and intentionally
 * generous negative space. Each ImageVector is built once, not on every
 * recomposition or animation frame. Compose Icon tint handles disabled/active
 * states without changing any interaction semantics.
 */
internal object NusaPlayerIcons {
    private val ink = SolidColor(Color.Black)

    private fun outline(name: String, draw: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = "Nusa-$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = PathBuilder().apply(draw).nodes,
                stroke = ink,
                strokeLineWidth = 1.85f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()

    private fun solid(name: String, draw: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = "Nusa-$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(pathData = PathBuilder().apply(draw).nodes, fill = ink)
        }.build()

    val Play = solid("play") {
        moveTo(8.05f, 4.85f)
        curveTo(7.35f, 4.4f, 6.5f, 4.9f, 6.5f, 5.85f)
        lineTo(6.5f, 18.15f)
        curveTo(6.5f, 19.1f, 7.35f, 19.6f, 8.05f, 19.15f)
        lineTo(19.0f, 12.95f)
        curveTo(19.7f, 12.55f, 19.7f, 11.45f, 19f, 11.05f)
        close()
    }
    val Pause = solid("pause") {
        moveTo(7.35f, 4.8f); lineTo(9.25f, 4.8f)
        quadTo(10.35f, 4.8f, 10.35f, 5.9f)
        lineTo(10.35f, 18.1f)
        quadTo(10.35f, 19.2f, 9.25f, 19.2f)
        lineTo(7.35f, 19.2f)
        quadTo(6.25f, 19.2f, 6.25f, 18.1f)
        lineTo(6.25f, 5.9f)
        quadTo(6.25f, 4.8f, 7.35f, 4.8f)
        close()
        moveTo(14.75f, 4.8f); lineTo(16.65f, 4.8f)
        quadTo(17.75f, 4.8f, 17.75f, 5.9f)
        lineTo(17.75f, 18.1f)
        quadTo(17.75f, 19.2f, 16.65f, 19.2f)
        lineTo(14.75f, 19.2f)
        quadTo(13.65f, 19.2f, 13.65f, 18.1f)
        lineTo(13.65f, 5.9f)
        quadTo(13.65f, 4.8f, 14.75f, 4.8f)
        close()
    }
    val Previous = solid("previous") {
        moveTo(5.25f, 5.7f); quadTo(5.25f, 4.75f, 6.15f, 4.75f)
        quadTo(7.05f, 4.75f, 7.05f, 5.7f)
        lineTo(7.05f, 18.3f)
        quadTo(7.05f, 19.25f, 6.15f, 19.25f)
        quadTo(5.25f, 19.25f, 5.25f, 18.3f)
        close()
        moveTo(18.15f, 5.65f)
        curveTo(18.7f, 5.3f, 19.25f, 5.65f, 19.25f, 6.35f)
        lineTo(19.25f, 17.65f)
        curveTo(19.25f, 18.35f, 18.7f, 18.7f, 18.15f, 18.35f)
        lineTo(8.7f, 12.7f)
        quadTo(8.05f, 12.25f, 8.7f, 11.3f)
        close()
    }
    val Next = solid("next") {
        moveTo(16.95f, 5.7f); quadTo(16.95f, 4.75f, 17.85f, 4.75f)
        quadTo(18.75f, 4.75f, 18.75f, 5.7f)
        lineTo(18.75f, 18.3f)
        quadTo(18.75f, 19.25f, 17.85f, 19.25f)
        quadTo(16.95f, 19.25f, 16.95f, 18.3f)
        close()
        moveTo(5.85f, 5.65f)
        curveTo(5.3f, 5.3f, 4.75f, 5.65f, 4.75f, 6.35f)
        lineTo(4.75f, 17.65f)
        curveTo(4.75f, 18.35f, 5.3f, 18.7f, 5.85f, 18.35f)
        lineTo(15.3f, 12.7f)
        quadTo(15.95f, 12.25f, 15.3f, 11.3f)
        close()
    }
    val Shuffle = outline("shuffle") {
        moveTo(4f, 6.4f); lineTo(6.5f, 6.4f)
        curveTo(11f, 6.4f, 12.5f, 17.6f, 17.7f, 17.6f)
        lineTo(20f, 17.6f)
        moveTo(16.9f, 14.7f); lineTo(20f, 17.6f); lineTo(16.9f, 20.4f)
        moveTo(4f, 17.6f); lineTo(6.5f, 17.6f)
        curveTo(8.6f, 17.6f, 9.8f, 15.25f, 11f, 12.7f)
        moveTo(13.2f, 9.55f)
        curveTo(14.3f, 7.8f, 15.45f, 6.4f, 17.7f, 6.4f)
        lineTo(20f, 6.4f)
        moveTo(16.9f, 3.6f); lineTo(20f, 6.4f); lineTo(16.9f, 9.3f)
    }
    val Repeat = outline("repeat") {
        moveTo(4.25f, 10f); lineTo(4.25f, 8.3f)
        quadTo(4.25f, 5.5f, 7.2f, 5.5f)
        lineTo(18.8f, 5.5f)
        moveTo(16.3f, 3.1f); lineTo(18.9f, 5.5f); lineTo(16.3f, 7.9f)
        moveTo(19.75f, 14f); lineTo(19.75f, 15.7f)
        quadTo(19.75f, 18.5f, 16.8f, 18.5f)
        lineTo(5.2f, 18.5f)
        moveTo(7.7f, 16.1f); lineTo(5.1f, 18.5f); lineTo(7.7f, 20.9f)
    }
    val RepeatOne = outline("repeat-one") {
        moveTo(4.25f, 10f); lineTo(4.25f, 8.3f)
        quadTo(4.25f, 5.5f, 7.2f, 5.5f)
        lineTo(18.8f, 5.5f)
        moveTo(16.3f, 3.1f); lineTo(18.9f, 5.5f); lineTo(16.3f, 7.9f)
        moveTo(19.75f, 14f); lineTo(19.75f, 15.7f)
        quadTo(19.75f, 18.5f, 16.8f, 18.5f)
        lineTo(5.2f, 18.5f)
        moveTo(7.7f, 16.1f); lineTo(5.1f, 18.5f); lineTo(7.7f, 20.9f)
        moveTo(11.3f, 10.05f); lineTo(12.5f, 9.4f); lineTo(12.5f, 14.5f)
    }
    val Heart = outline("heart") {
        moveTo(12f, 20.1f)
        curveTo(7.2f, 16.55f, 3.5f, 13.1f, 3.5f, 9f)
        curveTo(3.5f, 5.4f, 7.9f, 4.1f, 10.35f, 6.65f)
        lineTo(12f, 8.3f); lineTo(13.65f, 6.65f)
        curveTo(16.1f, 4.1f, 20.5f, 5.4f, 20.5f, 9f)
        curveTo(20.5f, 13.1f, 16.8f, 16.55f, 12f, 20.1f)
        close()
    }
    val HeartFilled = solid("heart-filled") {
        moveTo(12f, 20.1f)
        curveTo(7.2f, 16.55f, 3.5f, 13.1f, 3.5f, 9f)
        curveTo(3.5f, 5.4f, 7.9f, 4.1f, 10.35f, 6.65f)
        lineTo(12f, 8.3f); lineTo(13.65f, 6.65f)
        curveTo(16.1f, 4.1f, 20.5f, 5.4f, 20.5f, 9f)
        curveTo(20.5f, 13.1f, 16.8f, 16.55f, 12f, 20.1f)
        close()
    }
    val Lyrics = outline("lyrics") {
        moveTo(4f, 5f); quadTo(4f, 3.5f, 5.6f, 3.5f)
        lineTo(18.4f, 3.5f); quadTo(20f, 3.5f, 20f, 5f)
        lineTo(20f, 16f); quadTo(20f, 17.5f, 18.4f, 17.5f)
        lineTo(10f, 17.5f); lineTo(5.2f, 21f); lineTo(5.2f, 17.5f)
        quadTo(4f, 17.1f, 4f, 16f); close()
        moveTo(8f, 8.4f); lineTo(16f, 8.4f)
        moveTo(8f, 12.3f); lineTo(13.7f, 12.3f)
    }
    val Library = outline("library") {
        moveTo(4.8f, 5.3f); quadTo(4.8f, 4f, 6.2f, 4f)
        lineTo(17.8f, 4f); quadTo(19.2f, 4f, 19.2f, 5.3f)
        lineTo(19.2f, 18.7f); quadTo(19.2f, 20f, 17.8f, 20f)
        lineTo(6.2f, 20f); quadTo(4.8f, 20f, 4.8f, 18.7f)
        close()
        moveTo(9f, 8f); lineTo(15.1f, 8f)
        moveTo(9f, 11.5f); lineTo(15.1f, 11.5f)
        moveTo(9f, 15f); lineTo(13.2f, 15f)
    }
    val Artist = outline("artist") {
        moveTo(12f, 12.2f)
        curveTo(14.25f, 12.2f, 15.7f, 10.7f, 15.7f, 8.5f)
        curveTo(15.7f, 6.3f, 14.25f, 4.8f, 12f, 4.8f)
        curveTo(9.75f, 4.8f, 8.3f, 6.3f, 8.3f, 8.5f)
        curveTo(8.3f, 10.7f, 9.75f, 12.2f, 12f, 12.2f)
        close()
        moveTo(5.6f, 19.5f); curveTo(5.6f, 16.6f, 8.15f, 14.4f, 12f, 14.4f)
        curveTo(15.85f, 14.4f, 18.4f, 16.6f, 18.4f, 19.5f)
    }
    val Share = outline("share") {
        moveTo(8f, 12f); lineTo(16.2f, 7.6f)
        moveTo(8f, 12f); lineTo(16.2f, 16.4f)
        moveTo(7.8f, 9.9f)
        curveTo(6.55f, 9.9f, 5.5f, 10.85f, 5.5f, 12f)
        curveTo(5.5f, 13.15f, 6.55f, 14.1f, 7.8f, 14.1f)
        curveTo(9.05f, 14.1f, 10.1f, 13.15f, 10.1f, 12f)
        curveTo(10.1f, 10.85f, 9.05f, 9.9f, 7.8f, 9.9f)
        close()
        moveTo(17.3f, 4.2f)
        curveTo(16.05f, 4.2f, 15f, 5.15f, 15f, 6.3f)
        curveTo(15f, 7.45f, 16.05f, 8.4f, 17.3f, 8.4f)
        curveTo(18.55f, 8.4f, 19.6f, 7.45f, 19.6f, 6.3f)
        curveTo(19.6f, 5.15f, 18.55f, 4.2f, 17.3f, 4.2f)
        close()
        moveTo(17.3f, 15.6f)
        curveTo(16.05f, 15.6f, 15f, 16.55f, 15f, 17.7f)
        curveTo(15f, 18.85f, 16.05f, 19.8f, 17.3f, 19.8f)
        curveTo(18.55f, 19.8f, 19.6f, 18.85f, 19.6f, 17.7f)
        curveTo(19.6f, 16.55f, 18.55f, 15.6f, 17.3f, 15.6f)
        close()
    }
    val Back = outline("back") {
        moveTo(7f, 9.5f); lineTo(12f, 14.5f); lineTo(17f, 9.5f)
    }
    val More = outline("more") {
        moveTo(5f, 12f); lineTo(5.08f, 12f)
        moveTo(12f, 12f); lineTo(12.08f, 12f)
        moveTo(19f, 12f); lineTo(19.08f, 12f)
    }
}
