package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jdrms.bulletin.core.designsystem.theme.BulletinMotion
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal const val HOME_TRAY_LIFT_DP = 220f
internal const val HOME_RECEIPT_DWELL_MILLIS = 1300
private val TraySettleEasing = HomeSafeCubicBezierEasing(0.22f, 0.85f, 0.25f, 1f)

/** One progress value, observed by graphics layers only. No draft or business state lives here. */
internal class HomeTrayMotion(private val durationScale: Float) {
    val progress = Animatable(0f)
    var visible by mutableStateOf(false)
        private set
    private var job: Job? = null

    fun follow(dyDp: Float, open: Boolean, scope: CoroutineScope) {
        job?.cancel()
        visible = true
        job = scope.launch(HomeMotionClock) { progress.snapTo(homeTrayDragProgress(dyDp, open)) }
    }

    fun settle(open: Boolean, scope: CoroutineScope) {
        job?.cancel()
        if (open) visible = true
        job = scope.launch(HomeMotionClock) {
            progress.animateTo(
                if (open) 1f else 0f,
                tween(homeMotionMillis(BulletinMotion.LONG, durationScale), easing = TraySettleEasing)
            )
            if (!open) visible = false
        }
    }
}

internal fun homeTrayDragProgress(dyDp: Float, open: Boolean): Float =
    ((if (open) 1f else 0f) + dyDp / HOME_TRAY_LIFT_DP).coerceIn(0f, 1f)

internal fun homeTrayAlpha(progress: Float): Float = ((progress - 0.3f) * 1.5f).coerceIn(0f, 1f)

internal fun homeReceiptMatches(
    receiptId: Long,
    listingId: HomeItemId?,
    interaction: HomeInteractionState
): Boolean =
    interaction.card == CardState.Sending && interaction.listingId == listingId &&
        interaction.tray.sentReceiptId == receiptId
