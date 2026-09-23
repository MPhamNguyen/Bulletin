package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingPage
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSnapshot
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.application.pageFor
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory

class ListingsMarketplaceListingSource(
    private val listingsRepository: ListingsRepository
) : MarketplaceListingSource {
    override suspend fun getAvailableListings(request: MarketplacePageRequest): MarketplaceListingPage {
        return listingsRepository.getAllListings()
            .filter { it.status == ListingStatus.AVAILABLE }
            .map { listing ->
                MarketplaceListingSnapshot(
                    id = "listing:${listing.id.value}",
                    sellerId = listing.sellerId.value,
                    sellerName = listing.sellerName,
                    title = listing.title,
                    description = listing.description,
                    priceAmount = listing.price.amount,
                    priceCurrency = listing.price.currency,
                    category = listing.category.toMarketplaceCategory(),
                    createdAtMillis = listing.createdAtMillis
                )
            }
            .pageFor(request)
    }
}

private fun ListingCategory.toMarketplaceCategory(): MarketplaceCategory {
    return when (this) {
        ListingCategory.TEXTBOOKS -> MarketplaceCategory.TEXTBOOKS
        ListingCategory.ELECTRONICS -> MarketplaceCategory.ELECTRONICS
        ListingCategory.FURNITURE -> MarketplaceCategory.FURNITURE
        ListingCategory.CLOTHING -> MarketplaceCategory.CLOTHING
        ListingCategory.HOUSING -> MarketplaceCategory.HOUSING
        ListingCategory.OTHER -> MarketplaceCategory.OTHER
    }
}
