package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePrice
import com.jdrms.bulletin.domain.marketplace.domain.repository.MarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.domain.service.MarketplaceSearchPolicy

class SearchMarketplace(
    private val repository: MarketplaceRepository,
    private val listingSource: MarketplaceListingSource = EmptyMarketplaceListingSource,
    private val searchPolicy: MarketplaceSearchPolicy = MarketplaceSearchPolicy()
) {
    suspend fun getCatalog(): List<MarketplaceItem> {
        return currentCatalog()
    }

    suspend fun search(query: String, category: MarketplaceCategory?): List<MarketplaceItem> {
        return searchPolicy.filterItems(currentCatalog(), query, category)
    }

    suspend fun getById(id: MarketplaceItemId): MarketplaceItem? {
        return currentCatalog().find { it.id == id }
    }

    private suspend fun currentCatalog(): List<MarketplaceItem> {
        val publishedListings = listingSource.getAvailableListings().map { it.toMarketplaceItem() }
        return (publishedListings + repository.getCatalog()).distinctBy { it.id }
    }
}

private fun MarketplaceListingSnapshot.toMarketplaceItem(): MarketplaceItem {
    return MarketplaceItem(
        id = MarketplaceItemId(id),
        sellerId = sellerId,
        sellerName = sellerName,
        title = title,
        description = description,
        price = MarketplacePrice(priceAmount, priceCurrency),
        category = category,
        createdAtMillis = createdAtMillis
    )
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
