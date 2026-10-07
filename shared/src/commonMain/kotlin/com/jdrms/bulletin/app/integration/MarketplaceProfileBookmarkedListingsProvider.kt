package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.marketplace.application.GetMarketplaceListingBookmarks
import com.jdrms.bulletin.domain.marketplace.application.RemoveMarketplaceListingBookmark
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingBookmarksPageCursor
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListing
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListingRemover
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListingsPage
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListingsProvider
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarksPageCursor
import com.jdrms.bulletin.domain.profile.domain.model.UserId

class MarketplaceProfileBookmarkedListingsProvider(
    private val getMarketplaceListingBookmarks: GetMarketplaceListingBookmarks,
    private val removeMarketplaceListingBookmark: RemoveMarketplaceListingBookmark,
    private val listingsRepository: ListingsRepository? = null
) : ProfileBookmarkedListingsProvider, ProfileBookmarkedListingRemover {
    override suspend fun getBookmarkedListings(
        userId: UserId,
        cursor: ProfileBookmarksPageCursor?,
        pageSize: Int
    ): ProfileBookmarkedListingsPage {
        val page = getMarketplaceListingBookmarks.getPage(
            userId = userId.value,
            cursor = cursor?.let { ListingBookmarksPageCursor(it.offset) },
            pageSize = pageSize
        )
        val marketplaceListings = page.listings.associateBy { listing -> listing.id }
        val missingListingIds = page.listingIds.filterNot(marketplaceListings::containsKey).toSet()
        val listingsContextListings = if (missingListingIds.isEmpty()) {
            emptyMap()
        } else {
            listingsRepository?.getAllListings().orEmpty().associateBy { listing ->
                MarketplaceItemId("listing:${listing.id.value}")
            }
        }
        val translatedListings = page.listingIds.mapNotNull { itemId ->
            marketplaceListings[itemId]?.let { listing ->
                ProfileBookmarkedListing(
                    id = itemId.value,
                    title = listing.title,
                    sellerName = listing.sellerName,
                    price = listing.price.formatted,
                    category = listing.category.name,
                    description = listing.description,
                    condition = listing.condition,
                    photos = listing.photos,
                    sellerReputationScore = listing.sellerReputationScore
                )
            } ?: listingsContextListings[itemId]?.let { listing ->
                ProfileBookmarkedListing(
                    id = itemId.value,
                    title = listing.title,
                    sellerName = listing.sellerName,
                    price = listing.price.formatted,
                    category = listing.category.name,
                    description = listing.description,
                    condition = listing.condition.name,
                    photos = listing.images
                )
            }
        }
        return ProfileBookmarkedListingsPage(
            listings = translatedListings,
            nextCursor = page.nextCursor?.let { ProfileBookmarksPageCursor(it.offset) }
        )
    }

    override suspend fun removeBookmark(userId: UserId, listingId: String) {
        removeMarketplaceListingBookmark(userId.value, MarketplaceItemId(listingId)).getOrThrow()
    }
}
