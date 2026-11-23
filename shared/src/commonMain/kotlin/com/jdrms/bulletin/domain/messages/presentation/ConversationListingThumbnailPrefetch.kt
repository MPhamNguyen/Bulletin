package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import com.jdrms.bulletin.core.common.imageRequestUrl
import com.jdrms.bulletin.domain.messages.application.GetConversations
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

internal fun conversationListingPrefetchUrls(viewerId: SenderId?, conversations: List<Conversation>): List<String> {
    if (viewerId == null) return emptyList()
    return conversations.take(GetConversations.PAGE_SIZE).filter { it.includes(viewerId) }.mapNotNull {
        it.listingThumbnailUrl?.takeIf(String::isNotBlank)
    }.distinctBy(String::imageRequestUrl)
}

@Composable
internal fun ConversationListingThumbnailPrefetch(inbox: InboxState) {
    val context = LocalPlatformContext.current
    val pixels = with(LocalDensity.current) { ConversationListingImageSize.roundToPx() }
    val urls = conversationListingPrefetchUrls(inbox.viewerId, inbox.conversations)
    LaunchedEffect(inbox.viewerId, urls, pixels, context) {
        val loader = SingletonImageLoader.get(context)
        urls.forEach { loader.enqueue(conversationListingImageRequest(context, it, pixels)) }
    }
}
