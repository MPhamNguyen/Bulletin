package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePrice
import com.jdrms.bulletin.domain.marketplace.domain.repository.MarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.domain.service.MarketplaceSearchPolicy

class SearchMarketplace(
    private val repository: MarketplaceRepository,
    private val listingSource: MarketplaceListingSource = MarketplaceRepositoryListingSource(repository),
    private val searchPolicy: MarketplaceSearchPolicy = MarketplaceSearchPolicy()
) {
    suspend fun getPage(request: MarketplacePageRequest): MarketplaceItemPage {
        val page = listingSource.getAvailableListings(request)
        return MarketplaceItemPage(
            items = page.listings.map { it.toMarketplaceItem() },
            nextCursor = page.nextCursor
        )
    }

    suspend fun getCatalog(): List<MarketplaceItem> {
        return collectPages(query = "", category = null)
    }

    suspend fun search(query: String, category: MarketplaceCategory?): List<MarketplaceItem> {
        return collectPages(query, category)
    }

    fun filterCatalog(
        catalog: List<MarketplaceItem>,
        query: String,
        category: MarketplaceCategory?
    ): List<MarketplaceItem> {
        return searchPolicy.filterItems(catalog, query, category)
    }

    suspend fun getById(id: MarketplaceItemId): MarketplaceItem? {
        return repository.getItem(id)
    }

    private suspend fun collectPages(
        query: String,
        category: MarketplaceCategory?
    ): List<MarketplaceItem> {
        val items = mutableListOf<MarketplaceItem>()
        var cursor: MarketplacePageCursor? = null
        do {
            val page = getPage(
                MarketplacePageRequest(
                    query = query,
                    category = category,
                    cursor = cursor,
                    pageSize = MAX_MARKETPLACE_PAGE_SIZE
                )
            )
            items += page.items
            cursor = page.nextCursor
        } while (cursor != null)
        return items.distinctBy { it.id }
    }
}

data class MarketplaceItemPage(
    val items: List<MarketplaceItem>,
    val nextCursor: MarketplacePageCursor?
)

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

class ViewMarketplaceListing(
    private val repository: MarketplaceRepository
) {
    suspend fun viewListing(listingID: String): Listing {
        val result = repository.viewListing(listingID)
        return when (result) {
            is Result.Success -> result.data
            is Result.Error -> throw result.exception
        }
    }

    suspend operator fun invoke(listingID: String): Result<Listing> {
        return repository.viewListing(listingID)
    }
}
