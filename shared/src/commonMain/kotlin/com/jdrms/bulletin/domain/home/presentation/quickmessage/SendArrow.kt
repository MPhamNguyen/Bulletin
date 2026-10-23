package com.jdrms.bulletin.domain.home.presentation.quickmessage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
internal fun SendArrow(modifier: Modifier = Modifier) {
    Canvas(modifier.size(20.dp)) {
        val stroke = 2.5.dp.toPx()
        val centerX = size.width / 2f
        val tip = Offset(centerX, size.height * 0.18f)
        drawLine(QuickMessageTokens.ink, Offset(centerX, size.height * 0.82f), tip, stroke, cap = StrokeCap.Round)
        drawLine(
            QuickMessageTokens.ink,
            Offset(centerX - size.width * 0.3f, size.height * 0.46f),
            tip,
            stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            QuickMessageTokens.ink,
            Offset(centerX + size.width * 0.3f, size.height * 0.46f),
            tip,
            stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
internal fun DrawnCheckmark(progress: State<Float>) {
    Canvas(Modifier.size(14.dp)) {
        val value = progress.value
        val start = Offset(size.width * 0.15f, size.height * 0.52f)
        val corner = Offset(size.width * 0.42f, size.height * 0.8f)
        val end = Offset(size.width * 0.88f, size.height * 0.18f)
        val stroke = 2.dp.toPx()
        if (value > 0f) drawFirstCheckStroke(value, start, corner, stroke)
        if (value > 0.34f) drawSecondCheckStroke(value, corner, end, stroke)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFirstCheckStroke(
    progress: Float,
    start: Offset,
    corner: Offset,
    stroke: Float
) {
    val segment = (progress / 0.34f).coerceIn(0f, 1f)
    drawLine(
        QuickMessageTokens.teal,
        start,
        start + (corner - start) * segment,
        stroke,
        cap = StrokeCap.Round
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSecondCheckStroke(
    progress: Float,
    corner: Offset,
    end: Offset,
    stroke: Float
) {
    val segment = ((progress - 0.34f) / 0.66f).coerceIn(0f, 1f)
    drawLine(
        QuickMessageTokens.teal,
        corner,
        corner + (end - corner) * segment,
        stroke,
        cap = StrokeCap.Round
    )
}
