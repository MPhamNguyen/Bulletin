package com.jdrms.bulletin.domain.recommendations.domain.service

import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.recommendations.domain.model.Recommendation
import com.jdrms.bulletin.domain.recommendations.domain.model.RecommendationId
import com.jdrms.bulletin.domain.recommendations.domain.model.UserPreferences

class RecommendationRankingService {

    fun rankListings(listings: List<Listing>, preferences: UserPreferences): List<Recommendation> {
        return listings.map { listing ->
            var score = 1.0
            val reasons = mutableListOf<String>()

            if (preferences.preferredCategories.contains(listing.category)) {
                score += 2.5
                reasons.add("Matches preferred category (${listing.category.name})")
            }

            val maxP = preferences.maxPrice
            if (maxP != null && listing.price.amount <= maxP) {
                score += 1.5
                reasons.add("Within budget ($${listing.price.amount})")
            }

            reasons.add("Campus proximity signal (${preferences.campusLocation})")

            Recommendation(
                id = RecommendationId("rec_" + listing.id.value),
                listing = listing,
                score = score,
                reason = reasons.joinToString(" • ")
            )
        }.sortedByDescending { it.score }
    }
}
