package com.jdrms.bulletin.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jdrms.bulletin.app.di.AppContainer
import com.jdrms.bulletin.domain.messages.presentation.ConversationAvatarPrefetch
import com.jdrms.bulletin.domain.messages.presentation.ConversationListingThumbnailPrefetch

/** Keeps inbox-driven media warming independent of destination and app-root recomposition. */
@Composable
internal fun ConversationMediaPrefetchRoute(container: AppContainer, signedInId: String?) {
    if (signedInId == null) return
    val inbox by container.messages.inboxStateOwner.state.collectAsState()
    if (inbox.viewerId?.value == signedInId) {
        ConversationAvatarPrefetch(inbox, container.messages.messageSenderPhotoProvider)
        ConversationListingThumbnailPrefetch(inbox)
    }
}
