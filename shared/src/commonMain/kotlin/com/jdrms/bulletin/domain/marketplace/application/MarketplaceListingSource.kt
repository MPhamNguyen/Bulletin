package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.repository.MarketplaceRepository
import kotlin.time.Instant

data class MarketplaceListingSnapshot(
    val id: String,
    val sellerId: String,
    val sellerName: String,
    val title: String,
    val description: String,
    val priceAmount: Double,
    val priceCurrency: String,
    val category: MarketplaceCategory,
    val createdAtMillis: Long
)

data class MarketplacePageCursor(
    val createdAt: Instant,
    val itemId: String
)

data class MarketplacePageRequest(
    val query: String = "",
    val category: MarketplaceCategory? = null,
    val cursor: MarketplacePageCursor? = null,
    val pageSize: Int = DEFAULT_MARKETPLACE_PAGE_SIZE
) {
    init {
        require(pageSize in 1..MAX_MARKETPLACE_PAGE_SIZE) {
            "Marketplace page size must be between 1 and $MAX_MARKETPLACE_PAGE_SIZE."
        }
    }
}

data class MarketplaceListingPage(
    val listings: List<MarketplaceListingSnapshot>,
    val nextCursor: MarketplacePageCursor?
)

interface MarketplaceListingSource {
    suspend fun getAvailableListings(request: MarketplacePageRequest): MarketplaceListingPage
}

internal object EmptyMarketplaceListingSource : MarketplaceListingSource {
    override suspend fun getAvailableListings(request: MarketplacePageRequest): MarketplaceListingPage {
        return MarketplaceListingPage(emptyList(), null)
    }
}

class MarketplaceRepositoryListingSource(
    private val repository: MarketplaceRepository
) : MarketplaceListingSource {
    override suspend fun getAvailableListings(request: MarketplacePageRequest): MarketplaceListingPage {
        return repository.getCatalog().map { item ->
            MarketplaceListingSnapshot(
                id = item.id.value,
                sellerId = item.sellerId,
                sellerName = item.sellerName,
                title = item.title,
                description = item.description,
                priceAmount = item.price.amount,
                priceCurrency = item.price.currency,
                category = item.category,
                createdAtMillis = item.createdAtMillis
            )
        }.pageFor(request)
    }
}

fun List<MarketplaceListingSnapshot>.pageFor(request: MarketplacePageRequest): MarketplaceListingPage {
    val keywords = request.query.trim().lowercase().split(Regex("\\s+")).filter(String::isNotEmpty)
    val matchingListings = asSequence()
        .filter { listing -> request.category == null || listing.category == request.category }
        .filter { listing ->
            val searchableText = "${listing.title.lowercase()} ${listing.category.name.lowercase()}"
            keywords.all(searchableText::contains)
        }
        .filter { listing -> request.cursor == null || listing.isAfter(request.cursor) }
        .sortedWith(marketplaceListingComparator)
        .distinctBy(MarketplaceListingSnapshot::id)
        .take(request.pageSize + 1)
        .toList()
    val pageListings = matchingListings.take(request.pageSize)
    return MarketplaceListingPage(
        listings = pageListings,
        nextCursor = if (matchingListings.size > request.pageSize) {
            pageListings.lastOrNull()?.toCursor()
        } else {
            null
        }
    )
}

internal val marketplaceListingComparator =
    compareByDescending<MarketplaceListingSnapshot> { it.createdAtMillis }
        .thenByDescending { it.id }

private fun MarketplaceListingSnapshot.isAfter(cursor: MarketplacePageCursor): Boolean {
    val cursorMillis = cursor.createdAt.toEpochMilliseconds()
    return createdAtMillis < cursorMillis ||
        (createdAtMillis == cursorMillis && id < cursor.itemId)
}

internal fun MarketplaceListingSnapshot.toCursor(): MarketplacePageCursor {
    return MarketplacePageCursor(
        createdAt = Instant.fromEpochMilliseconds(createdAtMillis),
        itemId = id
    )
}

const val DEFAULT_MARKETPLACE_PAGE_SIZE = 20
const val MAX_MARKETPLACE_PAGE_SIZE = 50
