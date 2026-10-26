package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.app.application.GetVisibleMarketplaceListings
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.BookmarkedListingsPage
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingBookmarksPageCursor
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePrice
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId
import com.jdrms.bulletin.domain.marketplace.domain.repository.MarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.domain.service.MarketplaceSearchPolicy
import kotlinx.coroutines.CancellationException

class SearchMarketplace(
    private val repository: MarketplaceRepository,
    private val listingSource: MarketplaceListingSource = MarketplaceRepositoryListingSource(repository),
    private val searchPolicy: MarketplaceSearchPolicy = MarketplaceSearchPolicy(),
    private val blockedUserProvider: MarketplaceBlockedUserProvider? = null,
    private val reportedListingProvider: MarketplaceReportedListingProvider =
        EmptyMarketplaceReportedListingProvider,
    getVisibleListings: GetVisibleMarketplaceListings? = null
) {
    private val visibilityPolicy = getVisibleListings ?: GetVisibleMarketplaceListings(
        listingSource = listingSource,
        blockedUserProvider = blockedUserProvider,
        reportedListingProvider = reportedListingProvider
    )

    suspend fun getPage(request: MarketplacePageRequest): MarketplaceItemPage {
        val page = visibilityPolicy(request)
        return MarketplaceItemPage(page.listings.map { it.toMarketplaceItem() }, page.nextCursor)
    }

    suspend fun getCatalog(): List<MarketplaceItem> {
        return collectPages(query = "", category = null, condition = null)
    }

    suspend fun search(
        query: String,
        category: MarketplaceCategory?,
        condition: MarketplaceCondition? = null
    ): List<MarketplaceItem> {
        return collectPages(query, category, condition)
    }

    fun filterCatalog(
        catalog: List<MarketplaceItem>,
        query: String,
        category: MarketplaceCategory?,
        condition: MarketplaceCondition? = null
    ): List<MarketplaceItem> {
        return searchPolicy.filterItems(catalog, query, category, condition)
    }

    suspend fun getById(id: MarketplaceItemId): MarketplaceItem? {
        return repository.getItem(id)
    }

    private suspend fun collectPages(
        query: String,
        category: MarketplaceCategory?,
        condition: MarketplaceCondition?
    ): List<MarketplaceItem> {
        val items = mutableListOf<MarketplaceItem>()
        var cursor: MarketplacePageCursor? = null
        do {
            val page = getPage(
                MarketplacePageRequest(
                    query = query,
                    category = category,
                    condition = condition,
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

internal fun MarketplaceListingSnapshot.toMarketplaceItem(): MarketplaceItem {
    return MarketplaceItem(
        id = MarketplaceItemId(id),
        sellerId = sellerId,
        sellerName = sellerName,
        title = title,
        description = description,
        price = MarketplacePrice(priceAmount, priceCurrency),
        category = category,
        condition = condition,
        createdAtMillis = createdAtMillis,
        photos = photos,
        updatedAtMillis = updatedAtMillis
    )
}

class BookmarkMarketplaceListing(
    private val repository: MarketplaceRepository
) {
    suspend operator fun invoke(userId: String, itemId: MarketplaceItemId): Result<Unit> {
        return repository.bookmarkListing(userId, itemId)
    }
}

class GetMarketplaceListingBookmarks(
    private val repository: MarketplaceRepository
) {
    suspend fun getBookmarkedIds(userId: String): Set<MarketplaceItemId> {
        return repository.getBookmarkedItemIds(userId)
    }

    suspend fun getPage(
        userId: String,
        cursor: ListingBookmarksPageCursor? = null,
        pageSize: Int = DEFAULT_BOOKMARKS_PAGE_SIZE
    ): BookmarkedListingsPage {
        require(pageSize in 1..MAX_BOOKMARKS_PAGE_SIZE) {
            "Bookmark page size must be between 1 and $MAX_BOOKMARKS_PAGE_SIZE."
        }
        return repository.getBookmarkedListingsPage(userId, cursor, pageSize)
    }
}

class RemoveMarketplaceListingBookmark(
    private val repository: MarketplaceRepository
) {
    suspend operator fun invoke(userId: String, itemId: MarketplaceItemId): Result<Unit> {
        return repository.removeListingBookmark(userId, itemId)
    }
}

class ViewMarketplaceListing(
    private val repository: MarketplaceRepository,
    private val sellerProfileProvider: MarketplaceSellerProfileProvider = EmptyMarketplaceSellerProfileProvider,
    private val blockedUserProvider: MarketplaceBlockedUserProvider? = null,
    private val reportedListingProvider: MarketplaceReportedListingProvider =
        EmptyMarketplaceReportedListingProvider
) {
    suspend fun hasReview(listingId: String, reviewerId: String): Boolean =
        repository.hasReviewForListing(MarketplaceItemId(listingId.removePrefix("listing:")), reviewerId)

    suspend fun viewListing(listingID: String, viewerId: String? = null): Listing {
        val result = invoke(listingID, viewerId)
        return when (result) {
            is Result.Success -> result.data
            is Result.Error -> throw result.exception
        }
    }

    suspend operator fun invoke(listingID: String, viewerId: String? = null): Result<Listing> {
        return when (val result = repository.viewListing(listingID)) {
            is Result.Success -> {
                val isHidden = try {
                    viewerId != null && viewerId.isNotBlank() && (
                        blockedUserProvider?.getHiddenUserIds(viewerId)?.contains(result.data.sellerId) == true ||
                            result.data.id in reportedListingProvider.getReportedListingIds(
                                MarketplaceReporterId(viewerId)
                            )
                        )
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: IllegalStateException) {
                    return Result.Error(exception)
                }
                if (isHidden) {
                    Result.Error(IllegalStateException("This listing is unavailable."))
                } else {
                    Result.Success(result.data.withResolvedSellerName())
                }
            }
            is Result.Error -> result
        }
    }

    private suspend fun Listing.withResolvedSellerName(): Listing {
        val profile = sellerProfileProvider.getSellerProfile(sellerId)
        return if (profile != null) {
            copy(
                sellerName = profile.name?.takeIf(String::isNotBlank) ?: sellerName,
                sellerSchool = profile.school?.takeIf(String::isNotBlank) ?: sellerSchool,
                sellerAvatarUrl = profile.avatarUrl?.takeIf(String::isNotBlank) ?: sellerAvatarUrl,
                sellerMajor = profile.major?.takeIf(String::isNotBlank) ?: sellerMajor,
                sellerReputationScore = profile.reputationScore ?: sellerReputationScore,
                sellerReviewCount = profile.reviewCount ?: sellerReviewCount,
                sellerIsVerified = profile.isVerified
            )
        } else {
            this
        }
    }
}

data class MarketplaceSellerProfile(
    val sellerId: String = "",
    val name: String?,
    val school: String?,
    val major: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val reputationScore: Double? = null,
    val reviewCount: Int? = null,
    val reviews: List<MarketplaceSellerReview> = emptyList(),
    val isVerified: Boolean = true
)

data class MarketplaceSellerReview(
    val id: String,
    val reviewerName: String,
    val rating: Int,
    val comment: String
)

/** Provides seller profile details without coupling marketplace to a profile implementation. */
fun interface MarketplaceSellerProfileProvider {
    suspend fun getSellerProfile(sellerId: String): MarketplaceSellerProfile?
}

object EmptyMarketplaceSellerProfileProvider : MarketplaceSellerProfileProvider {
    override suspend fun getSellerProfile(sellerId: String): MarketplaceSellerProfile? = null
}

const val DEFAULT_BOOKMARKS_PAGE_SIZE = 20
const val MAX_BOOKMARKS_PAGE_SIZE = 50
