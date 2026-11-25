package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.designsystem.ItemDetailRow
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceListingPreview
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceListingPreviewContent
import com.jdrms.bulletin.domain.messages.application.ConversationListingPreview
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant

/** Preserve known participant photos, including their absence, while fresh listing details load. */
internal fun ConversationListingPreview.toMarketplaceDetailPreview(
    listingId: String,
    knownParticipants: List<ConversationParticipant> = emptyList()
): MarketplaceListingPreview? {
    val snapshot = details ?: return null
    val seller = knownParticipants.firstOrNull { it.id.value == snapshot.sellerId }
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
            editedAtMillis = snapshot.editedAtMillis
        ),
        sellerAvatarUrl = seller?.avatarUrl?.takeIf(String::isNotBlank),
        sellerAvatarKnown = seller != null
    )
}
