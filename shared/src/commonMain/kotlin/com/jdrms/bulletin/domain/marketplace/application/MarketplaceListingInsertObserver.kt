package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterestOwnerId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

fun interface MarketplaceListingInsertObserver {
    fun observe(ownerId: MarketplaceInterestOwnerId): Flow<MarketplaceItemId>
}

object EmptyMarketplaceListingInsertObserver : MarketplaceListingInsertObserver {
    override fun observe(ownerId: MarketplaceInterestOwnerId): Flow<MarketplaceItemId> = emptyFlow()
}
