package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinSkeletonBlock
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

/**
 * Placeholder for the listing detail content. It mirrors the loaded layout (photo, title and
 * price, badges, seller card, description, item details) so the swap to real content is stable.
 * Emitted into the detail column, which supplies the spacing between sections.
 */
@Composable
internal fun ColumnScope.ListingDetailSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Loading listing details" },
        verticalArrangement = Arrangement.spacedBy(BulletinSpacing.l)
    ) {
        BulletinSkeletonBlock(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            shape = MaterialTheme.shapes.medium
        )
        SkeletonTitleRow()
        ListingTagsSkeleton()
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SellerInfoCardSkeleton()
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        ListingDescriptionSkeleton()
        ListingItemDetailsSkeleton()
        ListingActionsSkeleton()
    }
}

@Composable
private fun SkeletonTitleRow() {
    val lineHeight = with(LocalDensity.current) { MaterialTheme.typography.headlineSmall.lineHeight.toDp() }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        BulletinSkeletonBlock(Modifier.fillMaxWidth(TITLE_WIDTH).height(lineHeight))
        BulletinSkeletonBlock(
            Modifier.width(72.dp).height(lineHeight),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private const val TITLE_WIDTH = 0.55f
