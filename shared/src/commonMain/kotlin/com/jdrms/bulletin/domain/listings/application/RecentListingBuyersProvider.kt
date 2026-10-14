package com.jdrms.bulletin.domain.listings.application

import com.jdrms.bulletin.domain.listings.domain.model.ListingId

data class RecentListingBuyer(val id: String, val displayName: String)

fun interface RecentListingBuyersProvider {
    suspend fun getRecentBuyers(listingId: ListingId, sellerId: String): List<RecentListingBuyer>
}
