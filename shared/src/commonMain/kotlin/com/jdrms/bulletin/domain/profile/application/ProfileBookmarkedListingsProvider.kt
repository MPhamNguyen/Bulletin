package com.jdrms.bulletin.domain.profile.application

import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlin.jvm.JvmInline

data class ProfileBookmarkedListing(
    val id: String,
    val title: String,
    val sellerName: String,
    val price: String,
    val category: String,
    val description: String = "",
    val condition: String = "GOOD",
    val photos: List<String> = emptyList(),
    val sellerReputationScore: Double? = null,
    val sellerId: String = ""
)

@JvmInline
value class ProfileBookmarksPageCursor(val offset: Int) {
    init {
        require(offset >= 0) { "Bookmark page offset cannot be negative." }
    }
}

data class ProfileBookmarkedListingsPage(
    val listings: List<ProfileBookmarkedListing>,
    val nextCursor: ProfileBookmarksPageCursor?
)

fun interface ProfileBookmarkedListingsProvider {
    suspend fun getBookmarkedListings(
        userId: UserId,
        cursor: ProfileBookmarksPageCursor?,
        pageSize: Int
    ): ProfileBookmarkedListingsPage
}

fun interface ProfileBookmarkedListingRemover {
    suspend fun removeBookmark(userId: UserId, listingId: String)
}
