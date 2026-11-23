package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.designsystem.ItemDetailRow
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceListingPreview
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceListingPreviewContent
import com.jdrms.bulletin.domain.messages.application.ConversationListingPreview

internal fun ConversationListingPreview.toMarketplaceDetailPreview(listingId: String): MarketplaceListingPreview? {
    val snapshot = details ?: return null
    return MarketplaceListingPreview(
        listingId = listingId,
        sellerId = snapshot.sellerId,
        sellerName = snapshot.sellerName,
        content = MarketplaceListingPreviewContent(
            title = title,
            price = formattedPrice,
            category = snapshot.category,
            condition = snapshot.condition,
            description = snapshot.description,
            photos = snapshot.photos,
            attributes = snapshot.attributes.map { ItemDetailRow(it.label, it.value) },
            createdAtMillis = snapshot.createdAtMillis,
            updatedAtMillis = snapshot.updatedAtMillis
        )
    )
}
