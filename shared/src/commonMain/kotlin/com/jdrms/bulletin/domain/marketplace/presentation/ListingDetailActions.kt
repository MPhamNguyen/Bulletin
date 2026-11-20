package com.jdrms.bulletin.domain.marketplace.presentation

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
import com.jdrms.bulletin.core.designsystem.skeleton
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

@Composable
internal fun ListingDetailActions(
    isBookmarked: Boolean,
    bookmarkErrorMessage: String?,
    callbacks: ListingDetailActionCallbacks,
    canBookmarkListing: Boolean,
    canReportListing: Boolean,
    isLoading: Boolean = false
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
                modifier = Modifier.weight(1f).skeleton(isLoading),
                enabled = !isLoading,
                variant = BulletinButtonVariant.Outlined
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (isBookmarked) "Bookmarked" else "Bookmark",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isBookmarked) "Bookmarked" else "Bookmark")
            }
        }
        BulletinButton(
            onClick = callbacks.onDismiss,
            modifier = Modifier.weight(1f).skeleton(isLoading),
            enabled = !isLoading,
            variant = BulletinButtonVariant.Primary
        ) {
            Text("Close")
        }
    }
    if (canReportListing) {
        BulletinButton(
            onClick = callbacks.onReportListing,
            modifier = Modifier.fillMaxWidth().skeleton(isLoading),
            enabled = !isLoading,
            variant = BulletinButtonVariant.DestructiveOutlined
        ) {
            Text("Report Listing")
        }
    }
}
