package com.jdrms.bulletin.app.navigation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.dp

internal object MarketplaceNavigationIcons {
    private const val ROOF_BOTTOM_Y = 9.5f
    private val ink = SolidColor(Color.Black)

    private val roof = PathBuilder().apply {
        moveTo(5.264f, 2.933f)
        lineTo(18.736f, 2.933f)
        lineTo(21.326f, 7.078f)
        lineTo(21.326f, ROOF_BOTTOM_Y)
        lineTo(2.674f, ROOF_BOTTOM_Y)
        lineTo(2.674f, 7.078f)
        close()
    }.nodes

    private val walls = PathBuilder().apply {
        moveTo(4.228f, ROOF_BOTTOM_Y)
        lineTo(4.228f, 20.031f)
        curveTo(4.228f, 20.653f, 4.642f, 21.067f, 5.264f, 21.067f)
        lineTo(18.736f, 21.067f)
        curveTo(19.358f, 21.067f, 19.772f, 20.653f, 19.772f, 20.031f)
        lineTo(19.772f, ROOF_BOTTOM_Y)
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
