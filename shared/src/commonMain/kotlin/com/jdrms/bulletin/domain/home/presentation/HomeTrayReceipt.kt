package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import bulletin.shared.generated.resources.*
import com.jdrms.bulletin.core.designsystem.BulletinExtras
import com.jdrms.bulletin.core.designsystem.BulletinSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeSentReceiptHandshake(state: HomeInteractionState, actions: HomeCardActions, durationScale: Float) {
    val latest by rememberUpdatedState(state)
    val receiptId = state.tray.sentReceiptId
    val announcements = LocalHomeAnnouncements.current
    val sent = stringResource(Res.string.home_sent)
    LaunchedEffect(state.listingId, receiptId, durationScale) {
        if (receiptId == null || !homeReceiptMatches(receiptId, state.listingId, state)) return@LaunchedEffect
        announcements.once("sent:$receiptId", sent)
        delay(homeMotionMillis(HOME_RECEIPT_DWELL_MILLIS, durationScale).toLong())
        if (homeReceiptMatches(receiptId, state.listingId, latest)) actions.onSentReceiptCompleted(receiptId)
    }
}

@Composable
internal fun HomeTrayReceipt(sent: Boolean, durationScale: Float) {
    val motion = rememberHomeReceiptMotion(sent, durationScale)
    Row(
        Modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.graphicsLayer { alpha = motion.second.value }, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Check, null, tint = BulletinExtras.colors.success, modifier = Modifier.size(18.dp))
            Text(stringResource(Res.string.home_sent), color = BulletinExtras.colors.success)
        }
        Text(
            stringResource(if (sent) Res.string.home_message_bubble else Res.string.home_sending),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.graphicsLayer {
                alpha = motion.first.value
                translationY = (1f - motion.first.value) * 52.dp.toPx()
                translationX = (1f - motion.first.value) * 10.dp.toPx()
                scaleX = 0.6f + 0.4f * motion.first.value
                scaleY = scaleX
                transformOrigin = TransformOrigin(1f, 1f)
            }.background(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.shapes.medium
            ).padding(BulletinSpacing.m)
        )
    }
}

@Composable
private fun rememberHomeReceiptMotion(
    sent: Boolean,
    durationScale: Float
): Pair<Animatable<Float, *>, Animatable<Float, *>> {
    val swoosh = remember { Animatable(0f) }
    val receipt = remember { Animatable(0f) }
    LaunchedEffect(durationScale) {
        withContext(HomeMotionClock) { swoosh.animateTo(1f, tween(homeMotionMillis(500, durationScale))) }
    }
    LaunchedEffect(sent, durationScale) {
        withContext(HomeMotionClock) {
            receipt.animateTo(if (sent) 1f else 0f, tween(homeMotionMillis(300, durationScale)))
        }
    }
    return swoosh to receipt
}
