package com.jdrms.bulletin.domain.home.domain.service

import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import com.jdrms.bulletin.domain.home.domain.model.UserPreferences

class HomeRankingPolicy {
    fun rankItems(items: List<HomeFeedItem>, preferences: UserPreferences): List<HomeFeedItem> {
        return items.map { item ->
            var calculatedScore = 50.0
            var reason = "Popular on your campus"

            if (preferences.preferredCategories.contains(item.category)) {
                calculatedScore += 35.0
                reason = "Matches your category preference (${item.category.name})"
            }

            if (preferences.maxPrice != null && item.price.amount <= preferences.maxPrice) {
                calculatedScore += 15.0
            }

            item.copy(score = calculatedScore, reason = reason)
        }.sortedByDescending { it.score }
    }
}
