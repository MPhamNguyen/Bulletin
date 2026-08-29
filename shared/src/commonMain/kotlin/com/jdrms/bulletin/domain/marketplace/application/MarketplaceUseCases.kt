package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.repository.MarketplaceRepository

class SearchMarketplace(
    private val repository: MarketplaceRepository
) {
    suspend fun getCatalog(): List<MarketplaceItem> {
        return repository.getCatalog()
    }

    suspend fun search(query: String, category: MarketplaceCategory?): List<MarketplaceItem> {
        return repository.search(query, category)
    }

    suspend fun getById(id: MarketplaceItemId): MarketplaceItem? {
        return repository.getItem(id)
    }
}

class ToggleSaveMarketplaceItem(
    private val repository: MarketplaceRepository
) {
    suspend operator fun invoke(userId: String, itemId: MarketplaceItemId): Result<Boolean> {
        return repository.toggleSaved(userId, itemId)
    }

    suspend fun getSavedIds(userId: String): Set<MarketplaceItemId> {
        return repository.getSavedItemIds(userId)
    }
}
