package com.jdrms.bulletin.domain.profile.application

import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlin.jvm.JvmInline

/** Display-ready item detail line (for example "RAM" to "16 GB") supplied by the integration layer. */
data class ProfileListingAttribute(val label: String, val value: String)

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
    val sellerId: String = "",
    val buyerId: String? = null,
    val isSold: Boolean = false,
    val createdAtMillis: Long = 0L,
    val attributes: List<ProfileListingAttribute> = emptyList()
)

@JvmInline
value class ProfileBookmarksPageCursor(val offset: Int) {
    init {
        require(offset >= 0) { "Bookmark page offset cannot be negative." }
    }
}

data class ProfileBookmarkedListingsPage(
    val listings: List<ProfileBookmarkedListing>,
    val nextCursor: ProfileBookmarksPageCursor?,
    val filterOptions: ProfileBookmarkedListingFilterOptions = ProfileBookmarkedListingFilterOptions()
)

data class ProfileBookmarkedListingFilterOptions(
    val categories: List<String> = emptyList(),
    val conditions: List<String> = emptyList()
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
