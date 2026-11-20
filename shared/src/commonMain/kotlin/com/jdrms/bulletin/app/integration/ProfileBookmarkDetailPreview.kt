package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.designsystem.ItemDetailRow
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceListingPreview
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceListingPreviewContent
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListing

internal fun ProfileBookmarkedListing.toMarketplaceDetailPreview() = MarketplaceListingPreview(
    listingId = id,
    sellerId = sellerId,
    sellerName = sellerName,
    content = MarketplaceListingPreviewContent(
        title,
        price,
        category,
        condition,
        description,
        photos,
        attributes.map { ItemDetailRow(it.label, it.value) },
        createdAtMillis
    )
)
