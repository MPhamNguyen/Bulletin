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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButtonDefaults

@Composable
internal fun ListingDetailActions(
    isBookmarked: Boolean,
    bookmarkErrorMessage: String?,
    onToggleBookmark: () -> Unit,
    onDismiss: () -> Unit,
    onReportListing: () -> Unit,
    canReportListing: Boolean
) {
    bookmarkErrorMessage?.let { message ->
        Text(text = message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onToggleBookmark,
            modifier = Modifier.weight(1f),
            colors = BulletinButtonDefaults.outlinedButtonColors(),
            border = BulletinButtonDefaults.outlinedButtonBorder()
        ) {
            Icon(
                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = if (isBookmarked) "Bookmarked" else "Bookmark",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isBookmarked) "Bookmarked" else "Bookmark")
        }
        Button(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            colors = BulletinButtonDefaults.buttonColors()
        ) {
            Text("Close")
        }
    }
    if (canReportListing) {
        OutlinedButton(
            onClick = onReportListing,
            modifier = Modifier.fillMaxWidth(),
            colors = BulletinButtonDefaults.destructiveOutlinedButtonColors(),
            border = BulletinButtonDefaults.destructiveOutlinedButtonBorder()
        ) {
            Text("Report Listing")
        }
    }
}
