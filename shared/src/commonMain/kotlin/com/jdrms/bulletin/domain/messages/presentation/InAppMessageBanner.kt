package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.jdrms.bulletin.core.designsystem.theme.BulletinElevation
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.messages.application.ConversationListingPreviewProvider
import com.jdrms.bulletin.domain.messages.application.IncomingMessageAlert
import com.jdrms.bulletin.domain.messages.application.MessageSenderPhotoProvider

private const val PREVIEW_MAX_LINES = 2
private val WHITESPACE_RUN = Regex("\\s+")

internal fun bannerSenderName(senderName: String): String = senderName.trim().ifBlank { "New message" }

internal fun bannerPreview(content: String): String = content.trim().replace(WHITESPACE_RUN, " ")

/** Message preview framed by the sender photo and the associated listing. */
@Composable
internal fun InAppMessageBanner(
    alert: IncomingMessageAlert,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    avatar: (@Composable () -> Unit)? = null,
    senderPhotos: MessageSenderPhotoProvider? = null,
    listingPreviews: ConversationListingPreviewProvider? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open conversation", role = Role.Button, onClick = onOpen)
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
                customActions = listOf(
                    CustomAccessibilityAction("Dismiss") {
                        onDismiss()
                        true
                    }
                )
            },
        shape = BulletinShapes.cardCompact,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = BulletinElevation.overlay
    ) {
        Row(
            Modifier.padding(BulletinSpacing.m),
            horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (avatar != null) avatar() else BannerSenderPhoto(alert, senderPhotos)
            Column(Modifier.weight(1f)) {
                Text(
                    text = bannerSenderName(alert.senderName),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = bannerPreview(alert.content),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = PREVIEW_MAX_LINES,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BannerListingImage(alert, listingPreviews)
        }
    }
}
