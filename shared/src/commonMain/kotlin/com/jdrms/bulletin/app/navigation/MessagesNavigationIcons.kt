package com.jdrms.bulletin.app.navigation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.dp

internal object MessagesNavigationIcons {
    private val ink = SolidColor(Color.Black)

    private val silhouette = PathBuilder().apply {
        moveTo(2.711f, 10.448f)
        lineTo(21.081f, 2.641f)
        curveTo(22f, 2.338f, 21.697f, 2.797f, 22f, 4.019f)
        lineTo(18.785f, 20.552f)
        curveTo(18.602f, 21.471f, 18.05f, 21.682f, 17.132f, 21.195f)
        lineTo(11.162f, 17.062f)
        curveTo(10.363f, 16.511f, 10.335f, 15.868f, 11.07f, 15.133f)
        lineTo(17.224f, 8.428f)
        curveTo(17.711f, 7.877f, 17.591f, 8.033f, 16.856f, 8.244f)
        lineTo(7.855f, 13.847f)
        curveTo(7.056f, 14.334f, 6.229f, 14.426f, 5.375f, 14.122f)
        lineTo(2.62f, 12.837f)
        curveTo(1.765f, 12.35f, 1.793f, 11.551f, 2.711f, 10.448f)
        close()
    }.nodes

    val filled: ImageVector = ImageVector.Builder(
        name = "PaperPlane",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        addPath(
            pathData = silhouette,
            fill = ink
        )
    }.build()
}
