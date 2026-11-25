package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.core.designsystem.ItemDetailRow
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.service.MarketplaceAttributeFormatter

/** Display data only; fresh details still control availability and actions. */
data class MarketplaceListingPreview(
    val listingId: String,
    val sellerId: String,
    val sellerName: String,
    val content: MarketplaceListingPreviewContent,
    val sellerAvatarUrl: String? = null,
    /** True when a null URL means no photo rather than an unresolved photo. */
    val sellerAvatarKnown: Boolean = false
) {
    val id: MarketplaceItemId get() = MarketplaceItemId(listingId)
}

data class MarketplaceListingPreviewContent(
    val title: String,
    val price: String,
    val category: String,
    val condition: String,
    val description: String,
    val photos: List<String>,
    val attributes: List<ItemDetailRow>,
    val createdAtMillis: Long,
    val editedAtMillis: Long? = null
)

internal fun MarketplaceItem.toDetailPreview() = MarketplaceListingPreview(
    listingId = id.value,
    sellerId = sellerId,
    sellerName = sellerName,
    content = MarketplaceListingPreviewContent(
        title = title,
        price = price.formatted,
        category = category.name,
        condition = condition.name,
        description = description,
        photos = photos,
        attributes = MarketplaceAttributeFormatter.linesFor(
            category,
            attributes
        ).map { ItemDetailRow(it.label, it.value) },
        createdAtMillis = createdAtMillis,
        editedAtMillis = editedAtMillis
    )
)
