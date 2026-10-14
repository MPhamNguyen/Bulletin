package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerSalesProvider
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceSellerId

class ListingsMarketplaceSellerSalesProvider(
    private val listingsRepository: ListingsRepository
) : MarketplaceSellerSalesProvider {
    override suspend fun countSoldListings(sellerId: MarketplaceSellerId): Int =
        listingsRepository.getSellerListings(SellerId(sellerId.value))
            .count { it.status == ListingStatus.SOLD }
}
