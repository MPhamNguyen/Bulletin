package com.jdrms.bulletin.app.navigation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

internal object MessagesNavigationIcons {

    private const val VIEWPORT_SIZE = 24f

    private const val OUTLINE_STROKE_WIDTH = 0.4f

    private val ink = SolidColor(Color.Black)

    val filled: ImageVector = buildIcon(
        name = "MessagesFilled",
        pathData = MESSAGES_FILLED_PATH,
    )

    val outline: ImageVector = buildIcon(
        name = "MessagesOutline",
        pathData = MESSAGES_OUTLINE_PATH,
        strokeWidth = OUTLINE_STROKE_WIDTH,
    )

    fun forState(selected: Boolean): ImageVector = if (selected) filled else outline

    private fun buildIcon(
        name: String,
        pathData: String,
        strokeWidth: Float = 0f,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = VIEWPORT_SIZE,
        viewportHeight = VIEWPORT_SIZE,
    ).addPath(
        pathData = PathParser().parsePathString(pathData).toNodes(),
        fill = ink,
        stroke = if (strokeWidth > 0f) ink else null,
        strokeLineWidth = strokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()
}
