package com.jdrms.bulletin.core.designsystem

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Compact phone sizing and animation timing for [BulletinToast]. */
object BulletinToastDefaults {
    val Container = Color(0xFF1F2A3C)
    val Content = Color.White
    val Accent = Color(0xFF74DD86)
    val Check = Color(0xFF1F2A3C)
    val MaxWidth = 320.dp
    val MinHeight = BulletinSpacing.control
    val IconSize = 28.dp
    val CheckSize = 16.dp
    val ContentGap = BulletinSpacing.s
    val HorizontalPadding = BulletinSpacing.l
    val VerticalPadding = BulletinSpacing.s

    internal const val POP_DELAY_MILLIS = 100L
    internal const val LABEL_DELAY_MILLIS = 150L
    internal const val CHECK_DELAY_MILLIS = 300L
    internal const val LABEL_MILLIS = 350
    internal const val CHECK_MILLIS = 400
    internal const val ENTER_MILLIS = 320
    internal const val EXIT_MILLIS = 220
}

/**
 * A dark pill with a green check that springs in and draws itself, followed by [message].
 * Re-plays its entrance whenever [message] changes. Show it with [BulletinToastHost] so it slides in and out.
 */
@Composable
fun BulletinToast(
    message: String,
    modifier: Modifier = Modifier,
    showSuccessIcon: Boolean = true,
    action: @Composable () -> Unit = {}
) {
    val pop = remember(message) { Animatable(0f) }
    val label = remember(message) { Animatable(0f) }
    val check = remember(message) { Animatable(0f) }
    LaunchedEffect(message) {
        launch {
            delay(BulletinToastDefaults.POP_DELAY_MILLIS)
            pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
        }
        launch {
            delay(BulletinToastDefaults.LABEL_DELAY_MILLIS)
            label.animateTo(1f, tween(BulletinToastDefaults.LABEL_MILLIS, easing = FastOutSlowInEasing))
        }
        launch {
            delay(BulletinToastDefaults.CHECK_DELAY_MILLIS)
            check.animateTo(1f, tween(BulletinToastDefaults.CHECK_MILLIS, easing = FastOutSlowInEasing))
        }
    }
    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .widthIn(max = BulletinToastDefaults.MaxWidth)
            .heightIn(min = BulletinToastDefaults.MinHeight)
            .background(BulletinToastDefaults.Container, CircleShape)
            .padding(
                horizontal = BulletinToastDefaults.HorizontalPadding,
                vertical = BulletinToastDefaults.VerticalPadding
            ),
        horizontalArrangement = Arrangement.spacedBy(BulletinToastDefaults.ContentGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showSuccessIcon) {
            Box(
                modifier = Modifier
                    .size(BulletinToastDefaults.IconSize)
                    .graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                    }
                    .background(BulletinToastDefaults.Accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(BulletinToastDefaults.CheckSize)) { drawCheck(check.value) }
            }
        }
        Column(Modifier.weight(1f, fill = false)) {
            Text(
                text = message,
                color = BulletinToastDefaults.Content,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = label.value
                        translationX = (1f - label.value) * -LABEL_SLIDE_DP.dp.toPx()
                    }
            )
            action()
        }
    }
}

/**
 * Hosts one [BulletinToast] near the top of its parent, sliding it in and out as [message] becomes non-null or
 * null. Place it last in a full-size `Box` and align it with `Modifier.align(Alignment.TopCenter)`.
 */
@Composable
fun BulletinToastHost(message: String?, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = message,
        modifier = modifier
            .statusBarsPadding()
            .padding(top = BulletinSpacing.m, start = BulletinSpacing.xl, end = BulletinSpacing.xl),
        transitionSpec = {
            (
                slideInVertically(tween(BulletinToastDefaults.ENTER_MILLIS)) { -it } +
                    fadeIn(tween(BulletinToastDefaults.ENTER_MILLIS)) +
                    scaleIn(tween(BulletinToastDefaults.ENTER_MILLIS), initialScale = ENTER_SCALE)
                ) togetherWith (
                slideOutVertically(tween(BulletinToastDefaults.EXIT_MILLIS)) { -it / 2 } +
                    fadeOut(tween(BulletinToastDefaults.EXIT_MILLIS)) +
                    scaleOut(tween(BulletinToastDefaults.EXIT_MILLIS), targetScale = EXIT_SCALE)
                )
        },
        label = "Bulletin toast"
    ) { text -> if (text != null) BulletinToast(text) }
}

private fun DrawScope.drawCheck(progress: Float) {
    if (progress <= 0f) return
    val unit = size.width / CHECK_VIEWBOX
    val path = Path().apply {
        moveTo(4.5f * unit, 12.5f * unit)
        lineTo(9.5f * unit, 17.5f * unit)
        lineTo(19.5f * unit, 6.5f * unit)
    }
    val measure = PathMeasure().apply { setPath(path, false) }
    val visible = Path()
    measure.getSegment(0f, measure.length * progress.coerceAtMost(1f), visible, true)
    drawPath(
        path = visible,
        color = BulletinToastDefaults.Check,
        style = Stroke(width = CHECK_STROKE * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private const val CHECK_VIEWBOX = 24f
private const val CHECK_STROKE = 3.2f
private const val LABEL_SLIDE_DP = 12
private const val ENTER_SCALE = 0.92f
private const val EXIT_SCALE = 0.96f
