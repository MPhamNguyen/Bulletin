package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import com.jdrms.bulletin.domain.messages.application.MessageSenderPhotoProvider
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlinx.coroutines.CancellationException

internal val LocalMessageSenderPhotos = staticCompositionLocalOf<MessageSenderPhotoProvider?> { null }

@Composable
internal fun MessageParticipantPhoto(
    name: String,
    senderId: SenderId?,
    modifier: Modifier = Modifier,
    initialUrl: String? = null
) {
    val provider = LocalMessageSenderPhotos.current
    val context = LocalPlatformContext.current
    val sizePixels = with(LocalDensity.current) { MessageAvatarSize.roundToPx() }
    val knownUrl = initialUrl?.takeIf(String::isNotBlank)
    key(senderId, provider, knownUrl) {
        val url by produceState(knownUrl) {
            if (knownUrl != null) return@produceState
            value = try {
                senderId?.let { provider?.photoUrl(it) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }
        Box(modifier.size(MessageAvatarSize).clip(BulletinShapes.pill)) {
            ConversationAvatar(name)
            url?.takeIf(String::isNotBlank)?.let { photoUrl ->
                SubcomposeAsyncImage(
                    model = messageAvatarImageRequest(context, photoUrl, sizePixels),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    loading = {},
                    error = {}
                )
            }
        }
    }
}
