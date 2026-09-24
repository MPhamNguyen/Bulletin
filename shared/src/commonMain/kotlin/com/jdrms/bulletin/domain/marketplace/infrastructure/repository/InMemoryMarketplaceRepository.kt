package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.repository.MarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.domain.service.MarketplaceSearchPolicy
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.MarketplaceItemDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.MarketplaceListingDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.mapper.MarketplaceMapper

class InMemoryMarketplaceRepository(
    private val searchPolicy: MarketplaceSearchPolicy = MarketplaceSearchPolicy(),
    initialListings: List<MarketplaceListingDto> = defaultSeedListings
) : MarketplaceRepository {

    private val listings = initialListings.map { MarketplaceMapper.toListingDomain(it) }.toMutableList()
    private val savedItemIdsByUser = mutableMapOf<String, MutableSet<MarketplaceItemId>>()

    override suspend fun getCatalog(): List<MarketplaceItem> {
        return listings.map { listing ->
            MarketplaceItem(
                id = listing.id,
                sellerId = listing.sellerId,
                sellerName = listing.sellerName,
                title = listing.title,
                description = listing.description,
                price = listing.price,
                category = listing.category,
                isSaved = listing.isSaved,
                createdAtMillis = listing.createdAtMillis
            )
        }
    }

    override suspend fun search(query: String, category: MarketplaceCategory?): List<MarketplaceItem> {
        val items = getCatalog()
        return searchPolicy.filterItems(items, query, category)
    }

    override suspend fun getItem(id: MarketplaceItemId): MarketplaceItem? {
        return getCatalog().find { it.id == id }
    }

    override suspend fun viewListing(listingID: String): Result<Listing> {
        val listing = listings.find { it.id.value == listingID }
        return if (listing != null) {
            Result.Success(listing.copy(isSaved = false))
        } else {
            Result.Error(NoSuchElementException("Listing not found with ID: $listingID"))
        }
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
        val defaultSeedListings = listOf(
            MarketplaceListingDto(
                id = "mkt_1",
                sellerId = "seller_101",
                sellerName = "Dominic Alfonso",
                title = "Calculus: Early Transcendentals (8th Ed)",
                description = "Great condition, minimal highlighting. Required for MATH 122/123.",
                price = 35.0,
                category = "TEXTBOOKS",
                condition = "GOOD",
                status = "AVAILABLE",
                photos = listOf("https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c"),
                reputationScore = 4.8
            ),
            MarketplaceListingDto(
                id = "mkt_2",
                sellerId = "seller_102",
                sellerName = "Sean Gallagher",
                title = "Sony WH-1000XM4 Noise Canceling Headphones",
                description = "Barely used, great for library studying. Includes case and cables.",
                price = 180.0,
                category = "ELECTRONICS",
                condition = "LIKE_NEW",
                status = "AVAILABLE",
                photos = listOf("https://images.unsplash.com/photo-1505740420928-5e560c06d30e"),
                reputationScore = 4.9
            ),
            MarketplaceListingDto(
                id = "mkt_3",
                sellerId = "seller_103",
                sellerName = "Jacob Ayoub",
                title = "Compact Dorm Mini Fridge (3.2 Cu. Ft)",
                description = "Clean, works perfectly with freezer compartment. Pick up on campus.",
                price = 70.0,
                category = "FURNITURE",
                condition = "GOOD",
                status = "AVAILABLE",
                photos = emptyList(),
                reputationScore = 4.5
            ),
            MarketplaceListingDto(
                id = "mkt_4",
                sellerId = "seller_104",
                sellerName = "Roger Carrillo",
                title = "CSULB Engineering Department Crewneck",
                description = "Size Medium, gold and black lettering. Very comfortable.",
                price = 20.0,
                category = "CLOTHING",
                condition = "LIKE_NEW",
                status = "AVAILABLE",
                photos = listOf("https://images.unsplash.com/photo-1556905055-8f358a7a47b2"),
                reputationScore = 5.0
            )
        )

        val defaultSeedItems = defaultSeedListings.map {
            MarketplaceItemDto(
                id = it.id,
                sellerId = it.sellerId,
                sellerName = it.sellerName,
                title = it.title,
                description = it.description,
                price = it.price,
                category = it.category,
                createdAtMillis = it.createdAtMillis
            )
        }
    }
}
