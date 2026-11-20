package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.messages.application.ConversationListingPreview

@Composable
internal fun ConversationListingCard(
    preview: ConversationListingPreview?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    sellAction: ListingSellAction? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (preview != null && onClick != null) {
                    Modifier.clickable(
                        role = Role.Button,
                        onClickLabel = "View listing",
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(BulletinSpacing.m)) {
            ListingSummaryRow(preview, showChevron = onClick != null)
            if (sellAction != null && preview != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = BulletinSpacing.m),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                SellToAction(sellAction.buyerName, sellAction.onClick)
            }
        }
    }
}

internal data class ListingSellAction(val buyerName: String, val onClick: () -> Unit)

@Composable
private fun ListingSummaryRow(preview: ConversationListingPreview?, showChevron: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
    ) {
        ConversationListingPreview(preview?.imageUrl, imageSize = 72.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BulletinSpacing.xs)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
            ) {
                Text(
                    preview?.title ?: "Listing",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (preview?.isActive == true) ListingActiveBadge()
            }
            Text(
                preview?.formattedPrice ?: "Listing information unavailable",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            preview?.categoryLabel?.let {
                Text(
                    "Listed in $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (preview != null && showChevron) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
