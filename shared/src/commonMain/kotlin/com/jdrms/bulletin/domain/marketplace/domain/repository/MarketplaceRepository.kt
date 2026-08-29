package com.jdrms.bulletin.domain.marketplace.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId

interface MarketplaceRepository {
    suspend fun getCatalog(): List<MarketplaceItem>
    suspend fun search(query: String, category: MarketplaceCategory?): List<MarketplaceItem>
    suspend fun getItem(id: MarketplaceItemId): MarketplaceItem?
    suspend fun toggleSaved(userId: String, itemId: MarketplaceItemId): Result<Boolean>
    suspend fun getSavedItemIds(userId: String): Set<MarketplaceItemId>
}
