package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId

internal fun interface MarketplaceListingIdSnapshotSource {
    suspend fun latestIds(): Set<MarketplaceItemId>
}
