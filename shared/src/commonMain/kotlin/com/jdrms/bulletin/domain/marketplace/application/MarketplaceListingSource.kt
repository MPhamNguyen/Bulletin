package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId
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
    val createdAtMillis: Long,
    val condition: MarketplaceCondition = MarketplaceCondition.UNKNOWN,
    val primaryPhotoUrl: String? = null,
    val photoUrls: List<String> = listOfNotNull(primaryPhotoUrl),
    val updatedAtMillis: Long = 0L
) {
    val photos: List<String>
        get() = photoUrls
}

data class MarketplacePageCursor(
    val createdAt: Instant,
    val itemId: String
)

data class MarketplacePageRequest(
    val query: String = "",
    val sellerId: String? = null,
    val category: MarketplaceCategory? = null,
    val condition: MarketplaceCondition? = null,
    val viewerId: String? = null,
    val cursor: MarketplacePageCursor? = null,
    val pageSize: Int = DEFAULT_MARKETPLACE_PAGE_SIZE
) {
    init {
        require(pageSize in 1..MAX_MARKETPLACE_PAGE_SIZE) {
            "Marketplace page size must be between 1 and $MAX_MARKETPLACE_PAGE_SIZE."
        }
    }
}

fun interface MarketplaceReportedListingProvider {
    suspend fun getReportedListingIds(reporterId: MarketplaceReporterId): Set<MarketplaceItemId>
}

object EmptyMarketplaceReportedListingProvider : MarketplaceReportedListingProvider {
    override suspend fun getReportedListingIds(reporterId: MarketplaceReporterId): Set<MarketplaceItemId> = emptySet()
}

data class MarketplaceListingPage(
    val listings: List<MarketplaceListingSnapshot>,
    val nextCursor: MarketplacePageCursor?
)

fun interface MarketplaceListingSource {
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
                createdAtMillis = item.createdAtMillis,
                condition = item.condition,
                primaryPhotoUrl = item.photos.firstOrNull(),
                photoUrls = item.photos,
                updatedAtMillis = item.updatedAtMillis
            )
        }.pageFor(request)
    }
}

fun List<MarketplaceListingSnapshot>.pageFor(request: MarketplacePageRequest): MarketplaceListingPage {
    val keywords = request.query.trim().lowercase().split(Regex("\\s+")).filter(String::isNotEmpty)
    val matchingListings = asSequence()
        .filter { listing -> request.viewerId == null || listing.sellerId != request.viewerId }
        .filter { listing -> request.sellerId == null || listing.sellerId == request.sellerId }
        .filter { listing -> request.category == null || listing.category == request.category }
        .filter { listing -> request.condition == null || listing.condition == request.condition }
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
