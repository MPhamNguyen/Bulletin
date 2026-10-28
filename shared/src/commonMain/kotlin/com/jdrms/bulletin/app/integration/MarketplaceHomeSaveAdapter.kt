package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.application.SaveListingFailure
import com.jdrms.bulletin.domain.home.application.SaveListingPort
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.marketplace.application.BookmarkMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId

/** One bookmark attempt for the supplied listing ID; session exclusions belong to the Home flow. */
class MarketplaceHomeSaveAdapter(private val bookmarkListing: BookmarkMarketplaceListing) : SaveListingPort {
    @Suppress("TooGenericExceptionCaught")
    override suspend fun save(userId: String, listingId: HomeItemId): Result<Unit> {
        if (userId.isBlank()) return Result.Error(SaveListingFailure.NotSignedIn())
        return try {
            when (val result = bookmarkListing(userId, MarketplaceItemId(listingId.value))) {
                is Result.Success -> Result.Success(Unit)
                is Result.Error -> Result.Error(saveFailure(result.exception))
            }
        } catch (exception: Exception) {
            Result.Error(saveFailure(exception))
        }
    }

    private fun saveFailure(exception: Throwable): SaveListingFailure = when (classifyHomeAdapterFailure(exception)) {
        HomeAdapterFailure.NOT_SIGNED_IN -> SaveListingFailure.NotSignedIn()
        HomeAdapterFailure.UNAVAILABLE -> SaveListingFailure.ListingUnavailable()
        HomeAdapterFailure.NETWORK -> SaveListingFailure.Network()
        HomeAdapterFailure.OWN_LISTING, HomeAdapterFailure.UNKNOWN -> SaveListingFailure.Unknown()
    }
}
