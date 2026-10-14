package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceSellerId

fun interface MarketplaceSellerSalesProvider {
    suspend fun countSoldListings(sellerId: MarketplaceSellerId): Int
}
