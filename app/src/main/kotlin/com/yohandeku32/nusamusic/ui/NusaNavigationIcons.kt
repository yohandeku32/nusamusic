package com.yohandeku32.nusamusic.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.dp

internal object NusaNavigationIcons {
    private val ink = SolidColor(Color.Black)
    private fun outline(name: String, path: PathBuilder.() -> Unit) = ImageVector.Builder(
        name = "NusaNavigation-$name", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        addPath(PathBuilder().apply(path).nodes, stroke = ink, strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
    }.build()

    val Discover = ImageVector.Builder(
        name = "NusaNavigation-Discover", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        addPath(PathBuilder().apply {
            moveTo(12f, 1.8f)
            curveTo(17.63f, 1.8f, 22.2f, 6.37f, 22.2f, 12f)
            curveTo(22.2f, 17.63f, 17.63f, 22.2f, 12f, 22.2f)
            curveTo(6.37f, 22.2f, 1.8f, 17.63f, 1.8f, 12f)
            curveTo(1.8f, 6.37f, 6.37f, 1.8f, 12f, 1.8f)
            close()
            moveTo(16.8f, 6.9f); lineTo(13.6f, 13.7f)
            lineTo(6.9f, 17f); lineTo(10.2f, 10.3f); close()
        }.nodes, fill = ink, pathFillType = PathFillType.EvenOdd)
    }.build()

    val Library = outline("Library") {
        moveTo(3.5f, 3f); lineTo(8f, 3f); lineTo(8f, 21f); lineTo(3.5f, 21f); close()
        moveTo(8f, 3f); lineTo(12.5f, 3f); lineTo(12.5f, 21f); lineTo(8f, 21f)
        moveTo(15.2f, 2.7f); lineTo(19f, 2f); lineTo(22f, 19.8f)
        lineTo(18.2f, 20.5f); close()
        moveTo(3.5f, 7f); lineTo(8f, 7f)
        moveTo(8f, 17f); lineTo(12.5f, 17f)
        moveTo(16.1f, 8f); lineTo(19.9f, 7.3f)
    }

    val Search = outline("Search") {
        moveTo(19f, 10.5f)
        curveTo(19f, 15.19f, 15.19f, 19f, 10.5f, 19f)
        curveTo(5.81f, 19f, 2f, 15.19f, 2f, 10.5f)
        curveTo(2f, 5.81f, 5.81f, 2f, 10.5f, 2f)
        curveTo(15.19f, 2f, 19f, 5.81f, 19f, 10.5f)
        close(); moveTo(16.6f, 16.6f); lineTo(22f, 22f)
    }

    val Account = outline("Account") {
        moveTo(22f, 12f)
        curveTo(22f, 17.52f, 17.52f, 22f, 12f, 22f)
        curveTo(6.48f, 22f, 2f, 17.52f, 2f, 12f)
        curveTo(2f, 6.48f, 6.48f, 2f, 12f, 2f)
        curveTo(17.52f, 2f, 22f, 6.48f, 22f, 12f); close()
        moveTo(16f, 9.2f)
        curveTo(16f, 11.41f, 14.21f, 13.2f, 12f, 13.2f)
        curveTo(9.79f, 13.2f, 8f, 11.41f, 8f, 9.2f)
        curveTo(8f, 6.99f, 9.79f, 5.2f, 12f, 5.2f)
        curveTo(14.21f, 5.2f, 16f, 6.99f, 16f, 9.2f); close()
        moveTo(5.5f, 19.4f); curveTo(6.5f, 14f, 17.5f, 14f, 18.5f, 19.4f)
    }
}
