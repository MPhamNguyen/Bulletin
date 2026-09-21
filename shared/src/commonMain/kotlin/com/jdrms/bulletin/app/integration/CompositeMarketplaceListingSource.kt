package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSnapshot
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource

class CompositeMarketplaceListingSource(
    private vararg val sources: MarketplaceListingSource
) : MarketplaceListingSource {
    override suspend fun getAvailableListings(): List<MarketplaceListingSnapshot> {
        return sources.flatMap { it.getAvailableListings() }.distinctBy { it.id }
    }
}
