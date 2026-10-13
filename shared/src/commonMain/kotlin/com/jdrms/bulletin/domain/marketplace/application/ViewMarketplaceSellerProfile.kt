package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem

data class MarketplaceSellerProfileDetails(
    val profile: MarketplaceSellerProfile,
    val listings: List<MarketplaceItem>
)

class ViewMarketplaceSellerProfile(
    private val profileProvider: MarketplaceSellerProfileProvider,
    private val listingSource: MarketplaceListingSource
) {
    suspend operator fun invoke(sellerId: String): MarketplaceSellerProfileDetails? {
        val profile = profileProvider.getSellerProfile(sellerId) ?: return null
        return MarketplaceSellerProfileDetails(profile, loadAvailableListings(sellerId))
    }

    private suspend fun loadAvailableListings(sellerId: String): List<MarketplaceItem> {
        val listings = mutableListOf<MarketplaceItem>()
        var cursor: MarketplacePageCursor? = null
        do {
            val page = listingSource.getAvailableListings(
                MarketplacePageRequest(cursor = cursor, pageSize = MAX_MARKETPLACE_PAGE_SIZE)
            )
            listings += page.listings
                .filter { it.sellerId == sellerId }
                .map { it.toMarketplaceItem() }
            cursor = page.nextCursor
        } while (cursor != null)
        return listings.distinctBy { it.id }
    }
}
