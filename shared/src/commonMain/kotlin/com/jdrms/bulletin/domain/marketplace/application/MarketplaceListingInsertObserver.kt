package com.jdrms.bulletin.domain.marketplace.application

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

fun interface MarketplaceListingInsertObserver {
    fun observe(): Flow<Unit>
}

object EmptyMarketplaceListingInsertObserver : MarketplaceListingInsertObserver {
    override fun observe(): Flow<Unit> = emptyFlow()
}
