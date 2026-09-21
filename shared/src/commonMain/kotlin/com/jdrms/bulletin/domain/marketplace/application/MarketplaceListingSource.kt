package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory

data class MarketplaceListingSnapshot(
    val id: String,
    val sellerId: String,
    val sellerName: String,
    val title: String,
    val description: String,
    val priceAmount: Double,
    val priceCurrency: String,
    val category: MarketplaceCategory,
    val createdAtMillis: Long
)

interface MarketplaceListingSource {
    suspend fun getAvailableListings(): List<MarketplaceListingSnapshot>
}

internal object EmptyMarketplaceListingSource : MarketplaceListingSource {
    override suspend fun getAvailableListings(): List<MarketplaceListingSnapshot> = emptyList()
}
