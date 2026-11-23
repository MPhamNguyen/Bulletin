package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.CancelListingReviewRequests
import com.jdrms.bulletin.domain.listings.application.CreateListingReviewRequest
import com.jdrms.bulletin.domain.listings.application.CurrentListingSellerProvider
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.application.MarkListingSold
import com.jdrms.bulletin.domain.listings.application.RestoreListingToMarketplace
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.messages.application.ConversationListingSales
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId

/** Translates the messages sale port into listings use cases for the signed-in seller. */
internal class ListingsConversationListingSales(
    private val sellers: CurrentListingSellerProvider,
    private val markListingSold: MarkListingSold,
    private val restoreListing: RestoreListingToMarketplace,
    private val createReviewRequest: CreateListingReviewRequest,
    private val cancelReviewRequests: CancelListingReviewRequests
) : ConversationListingSales {
    override suspend fun markSold(listingId: ListingReferenceId, buyerId: String): Result<Unit> {
        val id = listingId.toListingId()
        val seller = currentSeller() ?: return Result.Error(IllegalStateException(SELLER_UNAVAILABLE))
        return when (val sold = markListingSold(id, seller.id, buyerId)) {
            // The sale stands even if the review request fails; the seller can still restore or retry from My Listings.
            is Result.Success -> {
                createReviewRequest(sold.data, buyerId)
                Result.Success(Unit)
            }
            is Result.Error -> sold
        }
    }

    override suspend fun undoSale(listingId: ListingReferenceId): Result<Unit> {
        val id = listingId.toListingId()
        val seller = currentSeller() ?: return Result.Error(IllegalStateException(SELLER_UNAVAILABLE))
        return when (val restored = restoreListing(id, seller.id)) {
            is Result.Success -> when (val cancelled = cancelReviewRequests(id, seller.id)) {
                is Result.Success -> Result.Success(Unit)
                is Result.Error -> Result.Error(
                    cancelled.exception,
                    "Listing restored, but its review requests could not be cleared."
                )
            }
            is Result.Error -> restored
        }
    }

    private suspend fun currentSeller(): ListingSeller? = (sellers.getCurrentSeller() as? Result.Success)?.data

    // Conversations store raw IDs; accept marketplace-prefixed IDs for compatibility as well.
    private fun ListingReferenceId.toListingId(): ListingId =
        ListingId(value.removePrefix(MarketplaceConversationListingPreviewProvider.LISTING_ID_PREFIX))

    private companion object {
        const val SELLER_UNAVAILABLE = "Unable to load the current seller."
    }
}
