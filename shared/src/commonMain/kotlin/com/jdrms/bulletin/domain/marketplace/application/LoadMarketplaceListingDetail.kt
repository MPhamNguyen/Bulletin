package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.supervisorScope

data class MarketplaceListingDetail(val listing: Listing, val hasReview: Boolean)

/**
 * Loads everything the listing detail view shows. The listing, the seller profile, the viewer's
 * hidden-user and reported-listing lists, and the review check are independent read-only lookups,
 * so they start together and the wait is the slowest one rather than their sum.
 *
 * [sellerIdHint] lets the seller profile start before the listing returns. It is only used when the
 * fetched listing confirms the same seller; otherwise the profile is looked up afterwards.
 * Outcomes match [ViewMarketplaceListing]: a failed or hidden listing is an error and a
 * visibility lookup failure is only observed if the seller was not already blocked.
 */
class LoadMarketplaceListingDetail(private val viewListing: ViewMarketplaceListing) {

    suspend operator fun invoke(
        listingId: String,
        viewerId: String?,
        sellerIdHint: String? = null
    ): Result<MarketplaceListingDetail> = supervisorScope {
        val lookups = startLookups(listingId, viewerId, sellerIdHint)
        when (val result = lookups.listing.await()) {
            is Result.Error -> cancelPending(result)
            is Result.Success -> assemble(result.data, lookups, sellerIdHint)
        }
    }

    private fun CoroutineScope.startLookups(
        listingId: String,
        viewerId: String?,
        sellerIdHint: String?
    ): DetailLookups {
        val visibilityViewer = viewerId?.takeIf(String::isNotBlank)
        return DetailLookups(
            listing = async { viewListing.repository.viewListing(listingId) },
            sellerProfile = sellerIdHint?.let { id ->
                async { viewListing.sellerProfileProvider.getSellerProfile(id) }
            },
            hiddenUserIds = visibilityViewer?.let { id ->
                async { viewListing.blockedUserProvider?.getHiddenUserIds(id) }
            },
            reportedListingIds = visibilityViewer?.let { id ->
                async { viewListing.reportedListingProvider.getReportedListingIds(MarketplaceReporterId(id)) }
            },
            hasReview = viewerId?.let { id -> async { viewListing.hasReview(listingId, id) } }
        )
    }

    private suspend fun CoroutineScope.assemble(
        listing: Listing,
        lookups: DetailLookups,
        sellerIdHint: String?
    ): Result<MarketplaceListingDetail> {
        val hidden = try {
            lookups.isHidden(listing)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: IllegalStateException) {
            return cancelPending(Result.Error(exception))
        }
        if (hidden) return cancelPending(Result.Error(IllegalStateException("This listing is unavailable.")))
        val profile = if (lookups.sellerProfile != null && sellerIdHint == listing.sellerId) {
            lookups.sellerProfile.await()
        } else {
            lookups.sellerProfile?.cancel()
            viewListing.sellerProfileProvider.getSellerProfile(listing.sellerId)
        }
        val hasReview = lookups.hasReview?.await() ?: false
        return Result.Success(MarketplaceListingDetail(listing.withSellerProfile(profile), hasReview))
    }

    private fun <T : Result<Nothing>> CoroutineScope.cancelPending(result: T): T {
        coroutineContext.cancelChildren()
        return result
    }

    private class DetailLookups(
        val listing: Deferred<Result<Listing>>,
        val sellerProfile: Deferred<MarketplaceSellerProfile?>?,
        val hiddenUserIds: Deferred<Set<String>?>?,
        val reportedListingIds: Deferred<Set<MarketplaceItemId>>?,
        val hasReview: Deferred<Boolean>?
    ) {
        suspend fun isHidden(listing: Listing): Boolean =
            hiddenUserIds?.await()?.contains(listing.sellerId) == true ||
                reportedListingIds?.await()?.contains(listing.id) == true
    }
}
