package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinMotion
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

@Composable
internal fun ListingDetailActions(
    isBookmarked: Boolean,
    bookmarkErrorMessage: String?,
    callbacks: ListingDetailActionCallbacks,
    canBookmarkListing: Boolean,
    canReportListing: Boolean,
    isLoading: Boolean = false,
    bookmarkEnabled: Boolean = !isLoading
) {
    bookmarkErrorMessage?.takeIf { canBookmarkListing }?.let { message ->
        Text(text = message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
    ) {
        if (canBookmarkListing) {
            BulletinButton(
                onClick = callbacks.onToggleBookmark,
                modifier = Modifier.weight(1f),
                enabled = bookmarkEnabled,
                variant = BulletinButtonVariant.Outlined
            ) {
                Crossfade(isBookmarked, animationSpec = tween(BulletinMotion.STANDARD), label = "bookmark") { saved ->
                    Row {
                        Icon(
                            imageVector = if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (saved) "Bookmarked" else "Bookmark",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (saved) "Bookmarked" else "Bookmark")
                    }
                }
            }
        }
        BulletinButton(
            onClick = callbacks.onDismiss,
            modifier = Modifier.weight(1f),
            variant = BulletinButtonVariant.Primary
        ) {
            Text("Close")
        }
    }
    if (canReportListing) {
        BulletinButton(
            onClick = callbacks.onReportListing,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            variant = BulletinButtonVariant.DestructiveOutlined
        ) {
            Text("Report Listing")
        }
    }
}
