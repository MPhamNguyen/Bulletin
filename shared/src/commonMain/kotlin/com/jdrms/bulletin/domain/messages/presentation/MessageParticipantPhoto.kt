package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import com.jdrms.bulletin.domain.messages.application.MessageSenderPhotoProvider
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlinx.coroutines.CancellationException

internal val LocalMessageSenderPhotos = staticCompositionLocalOf<MessageSenderPhotoProvider?> { null }

@Composable
internal fun MessageParticipantPhoto(name: String, senderId: SenderId?, modifier: Modifier = Modifier) {
    val provider = LocalMessageSenderPhotos.current
    key(senderId, provider) {
        val url by produceState<String?>(null) {
            value = try {
                senderId?.let { provider?.photoUrl(it) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }
        if (url.isNullOrBlank()) {
            ConversationAvatar(name, modifier)
        } else {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = "$name avatar",
                modifier = modifier.size(40.dp).clip(BulletinShapes.pill),
                contentScale = ContentScale.Crop,
                loading = { ConversationAvatar(name) },
                error = { ConversationAvatar(name) }
            )
        }
    }
}
