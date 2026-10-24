package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.app.application.GetVisibleMarketplaceListings
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.application.ContactSellerFailure
import com.jdrms.bulletin.domain.home.application.ContactSellerPort
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.SellerMessage
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageCursor
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceListing
import com.jdrms.bulletin.domain.messages.application.MessageSeller
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

/**
 * Resolves seller identity inside the integration boundary and performs one send attempt.
 * MessageSeller has no idempotency key: retrying an ambiguously completed send can duplicate a message.
 * Feature 6 must account for that limitation when offering Retry; this adapter never retries itself.
 */
class MessagesHomeContactSellerAdapter(
    private val viewListing: ViewMarketplaceListing,
    private val getVisibleListings: GetVisibleMarketplaceListings,
    private val messageSeller: MessageSeller
) : ContactSellerPort {
    @Suppress("TooGenericExceptionCaught")
    override suspend fun send(userId: String, listingId: HomeItemId, message: SellerMessage): Result<Unit> {
        if (userId.isBlank()) return Result.Error(ContactSellerFailure.NotSignedIn())
        return try {
            sendOnce(userId, listingId, message)
        } catch (exception: Exception) {
            Result.Error(contactFailure(exception))
        }
    }

    private suspend fun sendOnce(userId: String, listingId: HomeItemId, message: SellerMessage): Result<Unit> {
        val listing = when (val result = viewListing(listingId.value)) {
            is Result.Success -> result.data
            is Result.Error -> return Result.Error(contactFailure(result.exception))
        }
        if (listing.sellerId == userId) return Result.Error(ContactSellerFailure.OwnListing())
        if (listing.isSold || listing.status != "AVAILABLE" ||
            !isVisible(userId, listingId, listing.sellerId)
        ) {
            return Result.Error(ContactSellerFailure.ListingUnavailable())
        }
        return when (
            val result = messageSeller(
                ListingReferenceId(listingId.value.removePrefix(LISTING_ID_PREFIX)),
                ConversationParticipant(SenderId(listing.sellerId), listing.sellerName),
                message.text
            )
        ) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> Result.Error(contactFailure(result.exception))
        }
    }

    private suspend fun isVisible(userId: String, listingId: HomeItemId, sellerId: String): Boolean {
        var cursor: MarketplacePageCursor? = null
        do {
            val page =
                getVisibleListings(MarketplacePageRequest(viewerId = userId, sellerId = sellerId, cursor = cursor))
            if (page.listings.any { it.id == listingId.value }) return true
            cursor = page.nextCursor
        } while (cursor != null)
        return false
    }

    private fun contactFailure(exception: Throwable): ContactSellerFailure =
        when (classifyHomeAdapterFailure(exception)) {
            HomeAdapterFailure.NOT_SIGNED_IN -> ContactSellerFailure.NotSignedIn()
            HomeAdapterFailure.UNAVAILABLE -> ContactSellerFailure.ListingUnavailable()
            HomeAdapterFailure.OWN_LISTING -> ContactSellerFailure.OwnListing()
            HomeAdapterFailure.NETWORK -> ContactSellerFailure.Network()
            HomeAdapterFailure.UNKNOWN -> ContactSellerFailure.Unknown()
        }

    private companion object {
        const val LISTING_ID_PREFIX = "listing:"
    }
}
