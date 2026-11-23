package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.ConversationListingPreview
import com.jdrms.bulletin.domain.messages.application.ConversationListingSales
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import kotlinx.coroutines.CompletableDeferred

internal const val SALE_LISTING_ID = "listing:1"

internal fun listingSalePreview(sold: Boolean = false) = ConversationListingPreview(
    title = "Desk",
    formattedPrice = "$10",
    imageUrl = "desk.jpg",
    categoryLabel = "Furniture",
    isActive = !sold,
    sellerId = "seller",
    isSold = sold,
    soldToBuyerId = "buyer".takeIf { sold }
)

internal class DeferredListingSales : ConversationListingSales {
    val result = CompletableDeferred<Result<Unit>>()
    val calls = mutableListOf<String>()

    override suspend fun markSold(listingId: ListingReferenceId, buyerId: String): Result<Unit> {
        calls += "sell:${listingId.value}:$buyerId"
        return result.await()
    }

    override suspend fun undoSale(listingId: ListingReferenceId): Result<Unit> {
        calls += "undo:${listingId.value}"
        return result.await()
    }
}
