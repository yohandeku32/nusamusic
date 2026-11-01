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
 * Soft terminals, rounded joins, balanced 2dp stroke, and intentionally
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
                strokeLineWidth = 2f,
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

    // Round-ended, softly weighted primary transport glyph.
    val Play = solid("play") {
        moveTo(8.05f, 4.6f)
        curveTo(7.02f, 3.98f, 6.05f, 4.54f, 6.05f, 5.8f)
        lineTo(6.05f, 18.2f)
        curveTo(6.05f, 19.46f, 7.02f, 20.02f, 8.05f, 19.4f)
        lineTo(19.1f, 12.98f)
        curveTo(19.9f, 12.53f, 19.9f, 11.47f, 19.1f, 11.02f)
        close()
    }
    val Pause = solid("pause") {
        moveTo(7.45f, 4.6f); lineTo(9.25f, 4.6f)
        quadTo(10.52f, 4.6f, 10.52f, 5.87f)
        lineTo(10.52f, 18.13f)
        quadTo(10.52f, 19.4f, 9.25f, 19.4f)
        lineTo(7.45f, 19.4f)
        quadTo(6.18f, 19.4f, 6.18f, 18.13f)
        lineTo(6.18f, 5.87f)
        quadTo(6.18f, 4.6f, 7.45f, 4.6f)
        close()
        moveTo(14.75f, 4.6f); lineTo(16.55f, 4.6f)
        quadTo(17.82f, 4.6f, 17.82f, 5.87f)
        lineTo(17.82f, 18.13f)
        quadTo(17.82f, 19.4f, 16.55f, 19.4f)
        lineTo(14.75f, 19.4f)
        quadTo(13.48f, 19.4f, 13.48f, 18.13f)
        lineTo(13.48f, 5.87f)
        quadTo(13.48f, 4.6f, 14.75f, 4.6f)
        close()
    }
    val Previous = solid("previous") {
        moveTo(5.3f, 4.65f); lineTo(5.95f, 4.65f)
        quadTo(7.15f, 4.65f, 7.15f, 5.85f)
        lineTo(7.15f, 18.15f)
        quadTo(7.15f, 19.35f, 5.95f, 19.35f)
        lineTo(5.3f, 19.35f)
        quadTo(4.1f, 19.35f, 4.1f, 18.15f)
        lineTo(4.1f, 5.85f)
        quadTo(4.1f, 4.65f, 5.3f, 4.65f)
        close()
        moveTo(18.05f, 5.1f)
        curveTo(18.99f, 4.53f, 19.9f, 5.1f, 19.9f, 6.28f)
        lineTo(19.9f, 17.72f)
        curveTo(19.9f, 18.9f, 18.99f, 19.47f, 18.05f, 18.9f)
        lineTo(8.55f, 12.97f)
        curveTo(7.81f, 12.5f, 7.81f, 11.5f, 8.55f, 11.03f)
        close()
    }
    val Next = solid("next") {
        moveTo(18.05f, 4.65f); lineTo(18.7f, 4.65f)
        quadTo(19.9f, 4.65f, 19.9f, 5.85f)
        lineTo(19.9f, 18.15f)
        quadTo(19.9f, 19.35f, 18.7f, 19.35f)
        lineTo(18.05f, 19.35f)
        quadTo(16.85f, 19.35f, 16.85f, 18.15f)
        lineTo(16.85f, 5.85f)
        quadTo(16.85f, 4.65f, 18.05f, 4.65f)
        close()
        moveTo(5.95f, 5.1f)
        curveTo(5.01f, 4.53f, 4.1f, 5.1f, 4.1f, 6.28f)
        lineTo(4.1f, 17.72f)
        curveTo(4.1f, 18.9f, 5.01f, 19.47f, 5.95f, 18.9f)
        lineTo(15.45f, 12.97f)
        curveTo(16.19f, 12.5f, 16.19f, 11.5f, 15.45f, 11.03f)
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
        // Rounded speech-sheet silhouette with music-lyrics lines.
        moveTo(6.25f, 3.75f); lineTo(17.75f, 3.75f)
        quadTo(20.35f, 3.75f, 20.35f, 6.35f)
        lineTo(20.35f, 15.05f)
        quadTo(20.35f, 17.65f, 17.75f, 17.65f)
        lineTo(10.45f, 17.65f)
        lineTo(5.15f, 20.95f)
        lineTo(5.35f, 17.55f)
        quadTo(3.65f, 16.95f, 3.65f, 15.05f)
        lineTo(3.65f, 6.35f)
        quadTo(3.65f, 3.75f, 6.25f, 3.75f)
        close()
        moveTo(7.9f, 8.45f); lineTo(16.1f, 8.45f)
        moveTo(7.9f, 12.3f); lineTo(13.65f, 12.3f)
    }
    val Library = outline("library") {
        // Rounded stacked cards instead of a rigid list box.
        moveTo(5.25f, 6.15f); lineTo(5.25f, 5.35f)
        quadTo(5.25f, 3.65f, 6.95f, 3.65f)
        lineTo(18.05f, 3.65f)
        quadTo(19.75f, 3.65f, 19.75f, 5.35f)
        lineTo(19.75f, 16f)
        moveTo(5.85f, 6.45f); lineTo(16.95f, 6.45f)
        quadTo(18.65f, 6.45f, 18.65f, 8.15f)
        lineTo(18.65f, 18.65f)
        quadTo(18.65f, 20.35f, 16.95f, 20.35f)
        lineTo(5.85f, 20.35f)
        quadTo(4.15f, 20.35f, 4.15f, 18.65f)
        lineTo(4.15f, 8.15f)
        quadTo(4.15f, 6.45f, 5.85f, 6.45f)
        close()
        moveTo(7.7f, 11.15f); lineTo(15.15f, 11.15f)
        moveTo(7.7f, 15.05f); lineTo(12.85f, 15.05f)
    }
    val Settings = outline("settings") {
        // A rounded 2dp outline gear from the same icon family as Artist,
        // Library and Home. No filled Material icon or mismatched weight.
        moveTo(10.35f, 2.75f); lineTo(13.65f, 2.75f)
        lineTo(14.25f, 5f); lineTo(15.82f, 5.68f)
        lineTo(17.78f, 4.48f); lineTo(20f, 6.72f)
        lineTo(18.82f, 8.68f); lineTo(19.52f, 10.3f)
        lineTo(21.75f, 10.85f); lineTo(21.75f, 13.15f)
        lineTo(19.52f, 13.7f); lineTo(18.82f, 15.32f)
        lineTo(20f, 17.28f); lineTo(17.78f, 19.52f)
        lineTo(15.82f, 18.32f); lineTo(14.25f, 19f)
        lineTo(13.65f, 21.25f); lineTo(10.35f, 21.25f)
        lineTo(9.75f, 19f); lineTo(8.18f, 18.32f)
        lineTo(6.22f, 19.52f); lineTo(4f, 17.28f)
        lineTo(5.18f, 15.32f); lineTo(4.48f, 13.7f)
        lineTo(2.25f, 13.15f); lineTo(2.25f, 10.85f)
        lineTo(4.48f, 10.3f); lineTo(5.18f, 8.68f)
        lineTo(4f, 6.72f); lineTo(6.22f, 4.48f)
        lineTo(8.18f, 5.68f); lineTo(9.75f, 5f)
        close()
        moveTo(15.15f, 12f)
        curveTo(15.15f, 13.74f, 13.74f, 15.15f, 12f, 15.15f)
        curveTo(10.26f, 15.15f, 8.85f, 13.74f, 8.85f, 12f)
        curveTo(8.85f, 10.26f, 10.26f, 8.85f, 12f, 8.85f)
        curveTo(13.74f, 8.85f, 15.15f, 10.26f, 15.15f, 12f)
        close()
    }
    val Home = outline("home") {
        // Soft, rounded roof and a clean doorway; coherent with the pill
        // icon family's 2dp stroke and rounded joins.
        moveTo(3.5f, 10.3f); lineTo(12f, 3.5f)
        lineTo(20.5f, 10.3f)
        moveTo(5.65f, 9.4f); lineTo(5.65f, 18.15f)
        quadTo(5.65f, 20.2f, 7.7f, 20.2f)
        lineTo(16.3f, 20.2f)
        quadTo(18.35f, 20.2f, 18.35f, 18.15f)
        lineTo(18.35f, 9.4f)
        moveTo(10f, 20.2f); lineTo(10f, 14.8f)
        quadTo(10f, 13.85f, 11f, 13.85f)
        lineTo(13f, 13.85f)
        quadTo(14f, 13.85f, 14f, 14.8f)
        lineTo(14f, 20.2f)
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
    // True circular dots, not tiny strokes whose caps depend on rendering.
    val More = solid("more") {
        moveTo(7.1f, 12f)
        curveTo(7.1f, 13.15f, 6.15f, 14.1f, 5f, 14.1f)
        curveTo(3.85f, 14.1f, 2.9f, 13.15f, 2.9f, 12f)
        curveTo(2.9f, 10.85f, 3.85f, 9.9f, 5f, 9.9f)
        curveTo(6.15f, 9.9f, 7.1f, 10.85f, 7.1f, 12f)
        close()
        moveTo(14.1f, 12f)
        curveTo(14.1f, 13.15f, 13.15f, 14.1f, 12f, 14.1f)
        curveTo(10.85f, 14.1f, 9.9f, 13.15f, 9.9f, 12f)
        curveTo(9.9f, 10.85f, 10.85f, 9.9f, 12f, 9.9f)
        curveTo(13.15f, 9.9f, 14.1f, 10.85f, 14.1f, 12f)
        close()
        moveTo(21.1f, 12f)
        curveTo(21.1f, 13.15f, 20.15f, 14.1f, 19f, 14.1f)
        curveTo(17.85f, 14.1f, 16.9f, 13.15f, 16.9f, 12f)
        curveTo(16.9f, 10.85f, 17.85f, 9.9f, 19f, 9.9f)
        curveTo(20.15f, 9.9f, 21.1f, 10.85f, 21.1f, 12f)
        close()
    }

}
