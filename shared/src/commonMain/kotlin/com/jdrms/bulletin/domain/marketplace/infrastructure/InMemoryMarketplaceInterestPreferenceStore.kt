package com.jdrms.bulletin.domain.marketplace.infrastructure

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceInterestPreferenceStore
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterest
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterestOwnerId

class InMemoryMarketplaceInterestPreferenceStore : MarketplaceInterestPreferenceStore {
    private val interests = mutableMapOf<MarketplaceInterestOwnerId, Set<MarketplaceInterest>>()

    override suspend fun get(ownerId: MarketplaceInterestOwnerId): Set<MarketplaceInterest> =
        interests[ownerId].orEmpty()

    override suspend fun set(ownerId: MarketplaceInterestOwnerId, interests: Set<MarketplaceInterest>) {
        this.interests[ownerId] = interests.toSet()
    }
}
