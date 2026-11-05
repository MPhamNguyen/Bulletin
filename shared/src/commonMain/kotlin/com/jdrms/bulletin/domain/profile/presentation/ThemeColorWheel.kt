package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
internal fun ThemeColorWheel(
    color: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier,
    wheelDiameter: Dp
) {
    val selectionCallback = rememberUpdatedState(onColorSelected)
    val selectedColor = rememberUpdatedState(color)
    val radius = with(LocalDensity.current) { wheelDiameter.toPx() / 2f }
    Box(
        modifier = modifier
            .size(wheelDiameter + 48.dp)
            .pointerInput(Unit) {
                detectTapGestures { point ->
                    selectionCallback.value(colorAt(point, this.size.width / 2f, radius, selectedColor.value))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { point ->
                        selectionCallback.value(colorAt(point, this.size.width / 2f, radius, selectedColor.value))
                    },
                    onDrag = { change, _ ->
                        selectionCallback.value(
                            colorAt(change.position, this.size.width / 2f, radius, selectedColor.value)
                        )
                    }
                )
            }
    ) {
        Canvas(Modifier.align(Alignment.Center).size(wheelDiameter).clip(CircleShape)) {
            val wheelRadius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val (hue, saturation, brightness) = themeWheelHueSaturationAndBrightness(color)
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        Color.Red,
                        Color.Yellow,
                        Color.Green,
                        Color.Cyan,
                        Color.Blue,
                        Color.Magenta,
                        Color.Red
                    ),
                    center
                ),
                radius = wheelRadius
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.Transparent),
                    center = center,
                    radius = wheelRadius
                ),
                radius = wheelRadius
            )
            drawCircle(
                color = Color.Black.copy(alpha = 1f - brightness),
                radius = wheelRadius
            )

            val angle = hue * (PI.toFloat() / 180f)
            val marker = Offset(
                x = center.x + cos(angle) * wheelRadius * saturation,
                y = center.y + sin(angle) * wheelRadius * saturation
            )
            drawCircle(Color.Black.copy(alpha = 0.72f), 11.dp.toPx(), marker)
            drawCircle(Color.White, 8.dp.toPx(), marker)
            drawCircle(color, 5.dp.toPx(), marker)
        }
    }
}

private fun colorAt(point: Offset, centerCoordinate: Float, radius: Float, selectedColor: Color): Color {
    val center = Offset(centerCoordinate, centerCoordinate)
    val dx = point.x - center.x
    val dy = point.y - center.y
    val saturation = (sqrt(dx * dx + dy * dy) / radius).coerceIn(0f, 1f)
    val hue = (atan2(dy, dx) * (180f / PI.toFloat()) + 360f) % 360f
    val selectedBrightness = themeWheelHueSaturationAndBrightness(selectedColor).third
    val brightness = if (selectedBrightness == 0f) 1f else selectedBrightness
    return themeWheelColorAt(hue, saturation, brightness)
}
