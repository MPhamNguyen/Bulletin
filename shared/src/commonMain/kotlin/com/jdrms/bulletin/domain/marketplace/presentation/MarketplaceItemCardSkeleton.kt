package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinSkeletonBlock
import com.jdrms.bulletin.core.designsystem.theme.BulletinElevation
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

/** Skeleton cards that fill the catalog grid while the first page of listings loads. */
internal fun LazyGridScope.marketplaceSkeletonItems(columns: Int, isVisible: Boolean) {
    if (!isVisible) return
    items(items = List(columns * SKELETON_ROWS) { it }, key = { "marketplace-skeleton-$it" }) {
        MarketplaceItemCardSkeleton(showDescription = columns == 1)
    }
}

/** One row of skeleton cards shown under the grid while the next page loads. */
@Composable
internal fun MarketplaceItemCardSkeletonRow(columns: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CARD_GAP)
    ) {
        repeat(columns) {
            Column(Modifier.weight(1f)) { MarketplaceItemCardSkeleton(showDescription = columns == 1) }
        }
    }
}

/** Mirrors [MarketplaceItemCard]: square photo, title and price, optional description, metadata. */
@Composable
internal fun MarketplaceItemCardSkeleton(showDescription: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().wrapContentHeight().clearAndSetSemantics { },
        shape = BulletinShapes.listingCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(defaultElevation = BulletinElevation.none)
    ) {
        BulletinSkeletonBlock(Modifier.fillMaxWidth().aspectRatio(1f), shape = MaterialTheme.shapes.extraSmall)
        Column(
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BulletinSkeletonBlock(Modifier.fillMaxWidth(TITLE_FRACTION).height(18.dp))
                BulletinSkeletonBlock(Modifier.fillMaxWidth(PRICE_FRACTION).height(18.dp))
            }
            if (showDescription) {
                BulletinSkeletonBlock(Modifier.fillMaxWidth().height(14.dp))
                BulletinSkeletonBlock(Modifier.fillMaxWidth(DESCRIPTION_FRACTION).height(14.dp))
            }
            BulletinSkeletonBlock(Modifier.fillMaxWidth(METADATA_FRACTION).height(12.dp))
        }
    }
}

private const val SKELETON_ROWS = 3
private const val TITLE_FRACTION = 0.5f
private const val PRICE_FRACTION = 0.5f
private const val DESCRIPTION_FRACTION = 0.7f
private const val METADATA_FRACTION = 0.6f
private val CARD_GAP = 14.dp
