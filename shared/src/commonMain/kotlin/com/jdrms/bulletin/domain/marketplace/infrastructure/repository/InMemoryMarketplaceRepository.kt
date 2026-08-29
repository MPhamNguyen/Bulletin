package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.repository.MarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.domain.service.MarketplaceSearchPolicy
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.MarketplaceItemDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.mapper.MarketplaceMapper

class InMemoryMarketplaceRepository(
    private val searchPolicy: MarketplaceSearchPolicy = MarketplaceSearchPolicy(),
    initialItems: List<MarketplaceItemDto> = defaultSeedItems
) : MarketplaceRepository {

    private val items = initialItems.map { MarketplaceMapper.toDomain(it) }.toMutableList()
    private val savedItemIdsByUser = mutableMapOf<String, MutableSet<MarketplaceItemId>>()

    override suspend fun getCatalog(): List<MarketplaceItem> {
        return items.toList()
    }

    override suspend fun search(query: String, category: MarketplaceCategory?): List<MarketplaceItem> {
        return searchPolicy.filterItems(items, query, category)
    }

    override suspend fun getItem(id: MarketplaceItemId): MarketplaceItem? {
        return items.find { it.id == id }
    }

    override suspend fun toggleSaved(userId: String, itemId: MarketplaceItemId): Result<Boolean> {
        val userSaved = savedItemIdsByUser.getOrPut(userId) { mutableSetOf() }
        val isSaved = if (userSaved.contains(itemId)) {
            userSaved.remove(itemId)
            false
        } else {
            userSaved.add(itemId)
            true
        }
        return Result.Success(isSaved)
    }

    override suspend fun getSavedItemIds(userId: String): Set<MarketplaceItemId> {
        return savedItemIdsByUser[userId]?.toSet() ?: emptySet()
    }

    companion object {
        private val defaultSeedItems = listOf(
            MarketplaceItemDto(
                id = "mkt_1",
                sellerId = "seller_101",
                sellerName = "Dominic Alfonso",
                title = "Calculus: Early Transcendentals (8th Ed)",
                description = "Great condition, minimal highlighting. Required for MATH 122/123.",
                price = 35.0,
                category = "TEXTBOOKS"
            ),
            MarketplaceItemDto(
                id = "mkt_2",
                sellerId = "seller_102",
                sellerName = "Sean Gallagher",
                title = "Sony WH-1000XM4 Noise Canceling Headphones",
                description = "Barely used, great for library studying. Includes case and cables.",
                price = 180.0,
                category = "ELECTRONICS"
            ),
            MarketplaceItemDto(
                id = "mkt_3",
                sellerId = "seller_103",
                sellerName = "Jacob Ayoub",
                title = "Compact Dorm Mini Fridge (3.2 Cu. Ft)",
                description = "Clean, works perfectly with freezer compartment. Pick up on campus.",
                price = 70.0,
                category = "FURNITURE"
            ),
            MarketplaceItemDto(
                id = "mkt_4",
                sellerId = "seller_104",
                sellerName = "Roger Carrillo",
                title = "CSULB Engineering Department Crewneck",
                description = "Size Medium, gold and black lettering. Very comfortable.",
                price = 20.0,
                category = "CLOTHING"
            )
        )
    }
}
