package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceSellerId

data class MarketplaceSellerProfileDetails(
    val profile: MarketplaceSellerProfile,
    val listings: List<MarketplaceItem>,
    val soldListingsCount: Int
)

class ViewMarketplaceSellerProfile(
    private val profileProvider: MarketplaceSellerProfileProvider,
    private val listingSource: MarketplaceListingSource,
    private val sellerSalesProvider: MarketplaceSellerSalesProvider
) {
    suspend operator fun invoke(sellerId: String): MarketplaceSellerProfileDetails? {
        val profile = profileProvider.getSellerProfile(sellerId) ?: return null
        return MarketplaceSellerProfileDetails(
            profile = profile,
            listings = loadAvailableListings(sellerId),
            soldListingsCount = sellerSalesProvider.countSoldListings(MarketplaceSellerId(sellerId))
        )
    }

    private suspend fun loadAvailableListings(sellerId: String): List<MarketplaceItem> {
        val listings = mutableListOf<MarketplaceItem>()
        var cursor: MarketplacePageCursor? = null
        do {
            val page = listingSource.getAvailableListings(
                MarketplacePageRequest(
                    sellerId = sellerId,
                    cursor = cursor,
                    pageSize = MAX_MARKETPLACE_PAGE_SIZE
                )
            )
            listings += page.listings
                .map { it.toMarketplaceItem() }
            cursor = page.nextCursor
        } while (cursor != null)
        return listings.distinctBy { it.id }
    }
}
