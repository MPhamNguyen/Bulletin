package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.runtime.Composable
import com.jdrms.bulletin.core.designsystem.ItemDetailRow
import com.jdrms.bulletin.core.designsystem.ItemDetailsSection
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributes
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.service.MarketplaceAttributeFormatter

/** Known, nonempty item attributes for a listing detail view; nothing is drawn when there are none. */
@Composable
internal fun MarketplaceItemDetails(listing: Listing) {
    MarketplaceItemDetails(listing.category, listing.attributes)
}

@Composable
internal fun MarketplaceItemDetails(category: MarketplaceCategory, attributes: MarketplaceAttributes) {
    val rows = MarketplaceAttributeFormatter.linesFor(category, attributes)
        .map { ItemDetailRow(it.label, it.value) }
    ItemDetailsSection(rows)
}
