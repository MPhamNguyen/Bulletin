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
        val listingsContextListings = loadMissingListings(missingListingIds)
        val translatedListings = page.listingIds.mapNotNull { itemId ->
            marketplaceListings[itemId]?.let { listing -> listing.toProfileBookmark(itemId) }
                ?: listingsContextListings[itemId]?.let { listing -> listing.toProfileBookmark(itemId) }
        }
        return ProfileBookmarkedListingsPage(
            listings = translatedListings,
            nextCursor = page.nextCursor?.let { ProfileBookmarksPageCursor(it.offset) }
        )
    }

    override suspend fun removeBookmark(userId: UserId, listingId: String) {
        removeMarketplaceListingBookmark(userId.value, MarketplaceItemId(listingId)).getOrThrow()
    }

    private suspend fun loadMissingListings(missingIds: Set<MarketplaceItemId>) =
        if (missingIds.isEmpty()) {
            emptyMap()
        } else {
            listingsRepository?.getAllListings().orEmpty().associateBy { listing ->
                MarketplaceItemId("listing:${listing.id.value}")
            }
        }

    private fun com.jdrms.bulletin.domain.marketplace.domain.model.Listing.toProfileBookmark(
        itemId: MarketplaceItemId
    ) = ProfileBookmarkedListing(
        id = itemId.value,
        title = title,
        sellerName = sellerName,
        price = price.formatted,
        category = category.name,
        description = description,
        condition = condition,
        photos = photos,
        sellerReputationScore = sellerReputationScore
    )

    private fun com.jdrms.bulletin.domain.listings.domain.model.Listing.toProfileBookmark(
        itemId: MarketplaceItemId
    ) = ProfileBookmarkedListing(
        id = itemId.value,
        title = title,
        sellerName = sellerName,
        price = price.formatted,
        category = category.name,
        description = description,
        condition = condition.name,
        photos = images
    )
}
