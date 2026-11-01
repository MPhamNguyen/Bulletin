package com.jdrms.bulletin.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import kotlin.math.abs
import kotlin.test.fail

internal const val PIXEL_TOLERANCE = 0.08f

internal fun Color.over(background: Color): Color = compositeOver(background)

internal fun ImageBitmap.pixel(x: Int, y: Int): Color = toPixelMap()[x, y]

internal fun assertPixelColor(label: String, expected: Color, actual: Color) {
    val close = abs(expected.red - actual.red) <= PIXEL_TOLERANCE &&
        abs(expected.green - actual.green) <= PIXEL_TOLERANCE &&
        abs(expected.blue - actual.blue) <= PIXEL_TOLERANCE
    if (!close) fail("$label: expected $expected but was $actual")
}
