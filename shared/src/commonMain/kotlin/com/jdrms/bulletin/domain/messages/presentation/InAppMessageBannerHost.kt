package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinMotion
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.messages.application.ConversationListingPreviewProvider
import com.jdrms.bulletin.domain.messages.application.IncomingMessageAlert
import com.jdrms.bulletin.domain.messages.application.MessageSenderPhotoProvider
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

internal const val IN_APP_BANNER_AUTO_DISMISS_MILLIS = 4_000L
private val SWIPE_DISMISS_DISTANCE = 48.dp

/**
 * Slides [alert] in from the top, dismisses it after [IN_APP_BANNER_AUTO_DISMISS_MILLIS] or an upward swipe,
 * and respects the safe area. It is stateless: the visible alert comes from the caller, so rotation cannot
 * bring a dismissed banner back. Place it at the top of a full-size box.
 */
@Composable
internal fun InAppMessageBannerHost(
    alert: IncomingMessageAlert?,
    onOpen: (IncomingMessageAlert) -> Unit,
    onDismiss: (MessageId) -> Unit,
    modifier: Modifier = Modifier,
    avatar: (@Composable () -> Unit)? = null,
    senderPhotos: MessageSenderPhotoProvider? = null,
    listingPreviews: ConversationListingPreviewProvider? = null
) {
    LaunchedEffect(alert?.messageId) {
        val current = alert ?: return@LaunchedEffect
        delay(IN_APP_BANNER_AUTO_DISMISS_MILLIS)
        onDismiss(current.messageId)
    }
    AnimatedContent(
        targetState = alert,
        modifier = modifier,
        transitionSpec = {
            (slideInVertically(tween(BulletinMotion.ENTER)) { -it } + fadeIn(tween(BulletinMotion.ENTER))) togetherWith
                (slideOutVertically(tween(BulletinMotion.STANDARD)) { -it } + fadeOut(tween(BulletinMotion.STANDARD)))
        },
        contentKey = { it?.messageId },
        label = "inAppMessageBanner"
    ) { shown ->
        if (shown != null) SwipeUpBanner(shown, onOpen, onDismiss, avatar, senderPhotos, listingPreviews)
    }
}

@Composable
private fun SwipeUpBanner(
    alert: IncomingMessageAlert,
    onOpen: (IncomingMessageAlert) -> Unit,
    onDismiss: (MessageId) -> Unit,
    avatar: (@Composable () -> Unit)?,
    senderPhotos: MessageSenderPhotoProvider?,
    listingPreviews: ConversationListingPreviewProvider?
) {
    var offsetY by remember(alert.messageId) { mutableFloatStateOf(0f) }
    val dismissDistance = with(LocalDensity.current) { SWIPE_DISMISS_DISTANCE.toPx() }
    val dragState = rememberDraggableState { delta -> offsetY = (offsetY + delta).coerceAtMost(0f) }
    InAppMessageBanner(
        alert = alert,
        onOpen = { onOpen(alert) },
        onDismiss = { onDismiss(alert.messageId) },
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = BulletinSpacing.l, vertical = BulletinSpacing.s)
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStopped = {
                    if (offsetY <= -dismissDistance) onDismiss(alert.messageId) else offsetY = 0f
                }
            ),
        avatar = avatar,
        senderPhotos = senderPhotos,
        listingPreviews = listingPreviews
    )
}
