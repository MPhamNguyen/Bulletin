package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal fun themeWheelColorAt(hue: Float, saturation: Float, brightness: Float = 1f): Color {
    val value = brightness.coerceIn(0f, 1f)
    val chroma = saturation.coerceIn(0f, 1f) * value
    val secondary = chroma * (1f - abs((hue / 60f) % 2f - 1f))
    val (red, green, blue) = when (hue.roundToInt() / 60) {
        0, 6 -> Triple(chroma, secondary, 0f)
        1 -> Triple(secondary, chroma, 0f)
        2 -> Triple(0f, chroma, secondary)
        3 -> Triple(0f, secondary, chroma)
        4 -> Triple(secondary, 0f, chroma)
        else -> Triple(chroma, 0f, secondary)
    }
    val valueOffset = value - chroma
    return Color(red + valueOffset, green + valueOffset, blue + valueOffset)
}

internal fun themeWheelHueAndSaturation(color: Color): Pair<Float, Float> =
    themeWheelHueSaturationAndBrightness(color).let { it.first to it.second }

internal fun themeWheelHueSaturationAndBrightness(color: Color): Triple<Float, Float, Float> {
    val red = color.red
    val green = color.green
    val blue = color.blue
    val maximum = max(red, max(green, blue))
    val minimum = min(red, min(green, blue))
    val delta = maximum - minimum
    val hue = when {
        delta == 0f -> 0f
        maximum == red -> (60f * ((green - blue) / delta) + 360f) % 360f
        maximum == green -> 60f * ((blue - red) / delta + 2f)
        else -> 60f * ((red - green) / delta + 4f)
    }
    val saturation = if (maximum == 0f) 0f else delta / maximum
    return Triple(hue, saturation, maximum)
}
