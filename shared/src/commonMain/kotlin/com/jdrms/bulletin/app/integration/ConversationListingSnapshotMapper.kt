package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.service.MarketplaceAttributeFormatter
import com.jdrms.bulletin.domain.messages.application.ConversationListingAttribute
import com.jdrms.bulletin.domain.messages.application.ConversationListingDetails
import com.jdrms.bulletin.domain.messages.application.ConversationListingPreview

internal fun Listing.toConversationPreview() = ConversationListingPreview(
    title = title,
    formattedPrice = price.formatted,
    imageUrl = images.firstOrNull(),
    details = ConversationListingDetails(
        sellerId = sellerId.value,
        sellerName = sellerName,
        category = category.name,
        condition = condition.name,
        description = description,
        photos = images,
        attributes = MarketplaceAttributeFormatter.linesFor(
            MarketplaceCategory.valueOf(category.name),
            attributes.toMarketplaceAttributes()
        ).map { ConversationListingAttribute(it.label, it.value) },
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
)

internal fun MarketplaceItem.toConversationPreview() = ConversationListingPreview(
    title = title,
    formattedPrice = price.formatted,
    imageUrl = photos.firstOrNull(),
    details = ConversationListingDetails(
        sellerId = sellerId,
        sellerName = sellerName,
        category = category.name,
        condition = condition.name,
        description = description,
        photos = photos,
        attributes = MarketplaceAttributeFormatter.linesFor(category, attributes)
            .map { ConversationListingAttribute(it.label, it.value) },
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
)
