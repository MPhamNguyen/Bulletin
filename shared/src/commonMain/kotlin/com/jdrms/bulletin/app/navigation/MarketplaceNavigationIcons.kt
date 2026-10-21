package com.jdrms.bulletin.app.navigation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.dp

internal object MarketplaceNavigationIcons {
    private val ink = SolidColor(Color.Black)

    private val roof = PathBuilder().apply {
        moveTo(5.264f, 2.933f)
        lineTo(18.736f, 2.933f)
        lineTo(21.326f, 7.078f)
        lineTo(21.326f, 9.15f)
        curveTo(21.326f, 10.705f, 20.021f, 11.948f, 18.808f, 11.223f)
        curveTo(18.342f, 10.808f, 18.249f, 10.808f, 17.782f, 11.223f)
        curveTo(16.943f, 11.845f, 16.197f, 11.845f, 15.358f, 11.223f)
        curveTo(14.891f, 10.808f, 14.798f, 10.808f, 14.332f, 11.223f)
        curveTo(13.492f, 11.845f, 12.746f, 11.845f, 12f, 11.223f)
        curveTo(11.534f, 10.808f, 11.44f, 10.808f, 10.974f, 11.223f)
        curveTo(10.135f, 11.845f, 9.389f, 11.845f, 8.549f, 11.223f)
        curveTo(8.083f, 10.808f, 7.99f, 10.808f, 7.523f, 11.223f)
        curveTo(6.684f, 11.845f, 5.938f, 11.845f, 5.192f, 11.223f)
        curveTo(3.979f, 11.948f, 2.674f, 10.705f, 2.674f, 9.15f)
        lineTo(2.674f, 7.078f)
        close()
    }.nodes

    private val walls = PathBuilder().apply {
        moveTo(4.228f, 11.534f)
        lineTo(4.228f, 20.031f)
        curveTo(4.228f, 20.653f, 4.642f, 21.067f, 5.264f, 21.067f)
        lineTo(18.736f, 21.067f)
        curveTo(19.358f, 21.067f, 19.772f, 20.653f, 19.772f, 20.031f)
        lineTo(19.772f, 11.534f)
    }.nodes

    private val doorway = PathBuilder().apply {
        moveTo(9.409f, 21.067f)
        lineTo(9.409f, 15.368f)
        curveTo(9.409f, 14.539f, 9.927f, 14.021f, 10.756f, 14.021f)
        lineTo(13.244f, 14.021f)
        curveTo(14.073f, 14.021f, 14.591f, 14.539f, 14.591f, 15.368f)
        lineTo(14.591f, 21.067f)
        close()
    }.nodes

    val outlined: ImageVector = createIcon(filled = false)
    val filled: ImageVector = createIcon(filled = true)

    private fun createIcon(filled: Boolean): ImageVector = ImageVector.Builder(
        name = if (filled) "SelectedMarketplaceStore" else "MarketplaceStore",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        addPath(
            pathData = walls,
            fill = null,
            stroke = ink,
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
        // Selection fills only the continuous roof and door; all contours are shared.
        listOf(roof, doorway).forEach { geometry ->
            addPath(
                pathData = geometry,
                fill = if (filled) ink else null,
                stroke = ink,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
    }.build()
}
