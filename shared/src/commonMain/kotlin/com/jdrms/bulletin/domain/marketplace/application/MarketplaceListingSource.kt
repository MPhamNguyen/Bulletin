package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributes
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePriceRange
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceSortOrder
import com.jdrms.bulletin.domain.marketplace.domain.model.supportsCondition
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
    val updatedAtMillis: Long = 0L,
    val attributes: MarketplaceAttributes = MarketplaceAttributes.EMPTY
) {
    val photos: List<String>
        get() = photoUrls
}

data class MarketplacePageCursor(
    val createdAt: Instant,
    val itemId: String,
    val priceAmount: Double? = null
)

data class MarketplacePageRequest(
    val query: String = "",
    val listingId: String? = null,
    val sellerId: String? = null,
    val category: MarketplaceCategory? = null,
    val condition: MarketplaceCondition? = null,
    val priceRange: MarketplacePriceRange = MarketplacePriceRange.NONE,
    val viewerId: String? = null,
    val cursor: MarketplacePageCursor? = null,
    val pageSize: Int = DEFAULT_MARKETPLACE_PAGE_SIZE,
    val sortOrder: MarketplaceSortOrder = MarketplaceSortOrder.NEWEST,
    val attributeFilters: MarketplaceAttributeFilters = MarketplaceAttributeFilters.NONE
) {
    init {
        require(pageSize in 1..MAX_MARKETPLACE_PAGE_SIZE) {
            "Marketplace page size must be between 1 and $MAX_MARKETPLACE_PAGE_SIZE."
        }
        require(attributeFilters.isEmpty || attributeFilters.isValidFor(category)) {
            "Attribute filters must match the selected category."
        }
        require(condition == null || category?.supportsCondition != false) {
            "Condition filters do not apply to Housing."
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
                updatedAtMillis = item.updatedAtMillis,
                attributes = item.attributes
            )
        }.pageFor(request)
    }
}

fun List<MarketplaceListingSnapshot>.pageFor(request: MarketplacePageRequest): MarketplaceListingPage {
    val matchingListings = asSequence()
        .filter { listing -> request.listingId == null || listing.id == request.listingId }
        .filter { listing -> request.viewerId == null || listing.sellerId != request.viewerId }
        .filter { listing -> request.sellerId == null || listing.sellerId == request.sellerId }
        .filter { listing -> request.category == null || listing.category == request.category }
        .filter { listing ->
            request.condition == null || listing.category.supportsCondition && listing.condition == request.condition
        }
        .filter { listing -> request.priceRange.contains(listing.priceAmount) }
        .filter { listing -> request.attributeFilters.matches(listing.attributes) }
        .filter { listing -> listing.matchesSearch(request.query) }
        .filter { listing -> request.cursor == null || listing.isAfter(request.cursor, request.sortOrder) }
        .sortedWith(request.sortOrder.listingComparator)
        .distinctBy(MarketplaceListingSnapshot::id)
        .take(request.pageSize + 1)
        .toList()
    val pageListings = matchingListings.take(request.pageSize)
    return MarketplaceListingPage(
        listings = pageListings,
        nextCursor = if (matchingListings.size > request.pageSize) {
            pageListings.lastOrNull()?.toCursor(request.sortOrder)
        } else {
            null
        }
    )
}

internal val marketplaceListingComparator =
    compareByDescending<MarketplaceListingSnapshot> { it.createdAtMillis }
        .thenByDescending { it.id }

internal val MarketplaceSortOrder.listingComparator: Comparator<MarketplaceListingSnapshot>
    get() = when (this) {
        MarketplaceSortOrder.NEWEST -> marketplaceListingComparator
        MarketplaceSortOrder.PRICE_LOW_TO_HIGH ->
            compareBy<MarketplaceListingSnapshot> { it.priceAmount }.thenBy { it.id }
        MarketplaceSortOrder.PRICE_HIGH_TO_LOW ->
            compareByDescending<MarketplaceListingSnapshot> { it.priceAmount }.thenByDescending { it.id }
    }

private fun MarketplaceListingSnapshot.isAfter(
    cursor: MarketplacePageCursor,
    sortOrder: MarketplaceSortOrder
): Boolean {
    if (sortOrder != MarketplaceSortOrder.NEWEST) {
        val cursorPrice = cursor.priceAmount
        if (cursorPrice != null) {
            return isAfterPrice(cursorPrice, cursor.itemId, sortOrder)
        }
    }
    val cursorMillis = cursor.createdAt.toEpochMilliseconds()
    return createdAtMillis < cursorMillis ||
        (createdAtMillis == cursorMillis && id < cursor.itemId)
}

private fun MarketplaceListingSnapshot.isAfterPrice(
    cursorPrice: Double,
    cursorId: String,
    sortOrder: MarketplaceSortOrder
): Boolean = when (sortOrder) {
    MarketplaceSortOrder.PRICE_LOW_TO_HIGH ->
        priceAmount > cursorPrice || (priceAmount == cursorPrice && id > cursorId)
    MarketplaceSortOrder.PRICE_HIGH_TO_LOW ->
        priceAmount < cursorPrice || (priceAmount == cursorPrice && id < cursorId)
    MarketplaceSortOrder.NEWEST -> false
}

internal fun MarketplaceListingSnapshot.toCursor(
    sortOrder: MarketplaceSortOrder = MarketplaceSortOrder.NEWEST
): MarketplacePageCursor {
    return MarketplacePageCursor(
        createdAt = Instant.fromEpochMilliseconds(createdAtMillis),
        itemId = id,
        priceAmount = priceAmount.takeUnless { sortOrder == MarketplaceSortOrder.NEWEST }
    )
}

const val DEFAULT_MARKETPLACE_PAGE_SIZE = 20
const val MAX_MARKETPLACE_PAGE_SIZE = 50
