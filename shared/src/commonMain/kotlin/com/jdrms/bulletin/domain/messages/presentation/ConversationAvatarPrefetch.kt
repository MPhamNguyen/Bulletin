package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import com.jdrms.bulletin.domain.messages.application.MessageSenderPhotoProvider

/** Mounted above destinations so the first inbox avatars warm while another screen is visible. */
@Composable
internal fun ConversationAvatarPrefetch(inbox: InboxState, photos: MessageSenderPhotoProvider) {
    val context = LocalPlatformContext.current
    val sizePixels = with(LocalDensity.current) { MessageAvatarSize.roundToPx() }
    val targets = conversationAvatarPrefetchTargets(inbox.viewerId, inbox.conversations)
    LaunchedEffect(inbox.viewerId, targets, sizePixels, photos, context) {
        val loader = SingletonImageLoader.get(context)
        prefetchConversationAvatars(targets, photos) { url ->
            loader.enqueue(messageAvatarImageRequest(context, url, sizePixels))
        }
    }
}
