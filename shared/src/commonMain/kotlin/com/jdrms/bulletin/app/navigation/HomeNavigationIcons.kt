package com.jdrms.bulletin.app.navigation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.dp

internal object HomeNavigationIcons {
    private val ink = SolidColor(Color.Black)

    private val silhouette = PathBuilder().apply {
        moveTo(3.5f, 10.15f)
        lineTo(3.5f, 21.5f)
        curveTo(3.5f, 21.78f, 3.72f, 22f, 4f, 22f)
        lineTo(8.5f, 22f)
        curveTo(8.78f, 22f, 9f, 21.78f, 9f, 21.5f)
        lineTo(9f, 16.35f)
        curveTo(9f, 15.6f, 9.6f, 15f, 10.35f, 15f)
        lineTo(13.65f, 15f)
        curveTo(14.4f, 15f, 15f, 15.6f, 15f, 16.35f)
        lineTo(15f, 21.5f)
        curveTo(15f, 21.78f, 15.22f, 22f, 15.5f, 22f)
        lineTo(20f, 22f)
        curveTo(20.28f, 22f, 20.5f, 21.78f, 20.5f, 21.5f)
        lineTo(20.5f, 10.15f)
        curveTo(20.5f, 9.98f, 20.43f, 9.82f, 20.31f, 9.7f)
        lineTo(12.39f, 2.19f)
        curveTo(12.17f, 1.98f, 11.83f, 1.98f, 11.61f, 2.19f)
        lineTo(3.69f, 9.7f)
        curveTo(3.57f, 9.82f, 3.5f, 9.98f, 3.5f, 10.15f)
        close()
    }.nodes

    val outlined: ImageVector = createIcon(filled = false)
    val filled: ImageVector = createIcon(filled = true)

    private fun createIcon(filled: Boolean): ImageVector = ImageVector.Builder(
        name = if (filled) "SelectedHome" else "Home",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        addPath(
            pathData = silhouette,
            fill = if (filled) ink else null,
            stroke = ink,
            strokeLineWidth = 2.2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }.build()
}
