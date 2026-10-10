package com.jdrms.bulletin.domain.marketplace.domain.model

/** The bookmarks owned by one marketplace user. */
class ListingBookmarks private constructor(
    val userId: String,
    private val listingIds: Set<MarketplaceItemId>
) {
    val ids: Set<MarketplaceItemId> get() = listingIds.toSet()

    fun bookmark(listingId: MarketplaceItemId): ListingBookmarks =
        ListingBookmarks(userId, listingIds + listingId)

    fun remove(listingId: MarketplaceItemId): ListingBookmarks =
        ListingBookmarks(userId, listingIds - listingId)

    companion object {
        fun of(userId: String, listingIds: Collection<MarketplaceItemId> = emptyList()): ListingBookmarks {
            require(userId.isNotBlank()) { "Bookmark owner cannot be blank." }
            require(listingIds.none { it.value.isBlank() }) { "Bookmarked listing ID cannot be blank." }
            return ListingBookmarks(userId, listingIds.toSet())
        }
    }
}

@JvmInline
value class ListingBookmarksPageCursor(val offset: Int) {
    init {
        require(offset >= 0) { "Bookmark page offset cannot be negative." }
    }
}

data class BookmarkedListingsPage(
    val listingIds: List<MarketplaceItemId>,
    val listings: List<Listing>,
    val nextCursor: ListingBookmarksPageCursor?
)
