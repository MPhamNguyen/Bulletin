package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterest
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterestOwnerId

interface MarketplaceInterestPreferenceStore {
    suspend fun get(ownerId: MarketplaceInterestOwnerId): Set<MarketplaceInterest>

    suspend fun set(ownerId: MarketplaceInterestOwnerId, interests: Set<MarketplaceInterest>)
}

class MarketplaceInterestPreferenceException(message: String, cause: Throwable) : Exception(message, cause)
