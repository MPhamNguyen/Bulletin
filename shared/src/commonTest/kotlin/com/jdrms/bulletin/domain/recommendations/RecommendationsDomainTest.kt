package com.jdrms.bulletin.domain.recommendations

import com.jdrms.bulletin.domain.marketplace.domain.model.*
import com.jdrms.bulletin.domain.recommendations.domain.model.UserPreferences
import com.jdrms.bulletin.domain.recommendations.domain.service.RecommendationRankingService
import kotlin.test.Test
import kotlin.test.assertEquals

class RecommendationsDomainTest {

    private val rankingService = RecommendationRankingService()

    @Test
    fun testRecommendationRankingBoostsPreferredCategory() {
        val prefs = UserPreferences(preferredCategories = listOf(Category.TEXTBOOKS), maxPrice = 100.0)

        val textbookListing = Listing(
            id = ListingId("item_1"),
            sellerId = SellerId("seller_1"),
            sellerName = "Dominic",
            title = "CECS 491 Textbook",
            description = "Book",
            price = Price(50.0),
            category = Category.TEXTBOOKS
        )

        val furnitureListing = Listing(
            id = ListingId("item_2"),
            sellerId = SellerId("seller_2"),
            sellerName = "Sean",
            title = "Desk Chair",
            description = "Chair",
            price = Price(50.0),
            category = Category.FURNITURE
        )

        val ranked = rankingService.rankListings(listOf(furnitureListing, textbookListing), prefs)

        assertEquals("item_1", ranked.first().listing.id.value)
    }
}
