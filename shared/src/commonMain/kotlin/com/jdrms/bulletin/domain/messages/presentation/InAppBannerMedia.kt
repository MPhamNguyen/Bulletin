package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import com.jdrms.bulletin.domain.messages.application.ConversationListingPreviewProvider
import com.jdrms.bulletin.domain.messages.application.IncomingMessageAlert
import com.jdrms.bulletin.domain.messages.application.MessageSenderPhotoProvider
import kotlinx.coroutines.CancellationException

@Composable
internal fun BannerSenderPhoto(alert: IncomingMessageAlert, provider: MessageSenderPhotoProvider?) {
    val url by produceState<String?>(null, alert.messageId, provider) {
        value = optionalBannerImage { alert.senderId?.let { provider?.photoUrl(it) } }
    }
    val name = bannerSenderName(alert.senderName)
    if (url.isNullOrBlank()) {
        ConversationAvatar(name)
    } else {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = "$name avatar",
            modifier = Modifier.size(40.dp).clip(BulletinShapes.pill),
            contentScale = ContentScale.Crop,
            loading = { ConversationAvatar(name) },
            error = { ConversationAvatar(name) }
        )
    }
}

@Composable
internal fun BannerListingImage(alert: IncomingMessageAlert, provider: ConversationListingPreviewProvider?) {
    val url by produceState<String?>(null, alert.messageId, provider) {
        value = optionalBannerImage { alert.listingId?.let { provider?.preview(it)?.imageUrl } }
    }
    if (alert.listingId != null) ConversationListingPreview(url, imageSize = 48.dp)
}

/** Optional artwork must never interrupt message delivery. */
private suspend fun optionalBannerImage(load: suspend () -> String?): String? = try {
    load()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    null
}
