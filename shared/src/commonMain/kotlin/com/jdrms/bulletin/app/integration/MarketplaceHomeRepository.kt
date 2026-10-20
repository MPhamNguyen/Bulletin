package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.app.application.GetVisibleMarketplaceListings
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCategory
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCursor
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedPage
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.HomePrice
import com.jdrms.bulletin.domain.home.domain.model.UserPreferences
import com.jdrms.bulletin.domain.home.domain.repository.HomeRepository
import com.jdrms.bulletin.domain.home.domain.service.HomeRankingPolicy
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageCursor
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest

class MarketplaceHomeRepository(
    private val getVisibleListings: GetVisibleMarketplaceListings,
    private val rankingPolicy: HomeRankingPolicy = HomeRankingPolicy()
) : HomeRepository {
    private val preferencesByUser = mutableMapOf<String, UserPreferences>()

    override suspend fun getFeed(
        userId: String?,
        cursor: HomeFeedCursor?,
        pageSize: Int
    ): HomeFeedPage {
        val page = getVisibleListings(
            MarketplacePageRequest(
                viewerId = userId,
                cursor = cursor?.toMarketplaceCursor(),
                pageSize = pageSize
            )
        )
        val feedItems = page.listings.map { listing ->
            HomeFeedItem(
                id = HomeItemId(listing.id),
                title = listing.title,
                description = listing.description,
                price = HomePrice(listing.priceAmount, listing.priceCurrency),
                category = HomeFeedCategory.valueOf(listing.category.name),
                createdAtMillis = listing.createdAtMillis,
                imageUrl = listing.primaryPhotoUrl,
                photoUrls = listing.photoUrls
            )
        }
        val preferences = getPreferences(userId)
        return HomeFeedPage(
            items = rankingPolicy.rankItems(feedItems, preferences),
            nextCursor = page.nextCursor?.let { HomeFeedCursor(it.createdAt, HomeItemId(it.itemId)) }
        )
    }

    override suspend fun getPreferences(userId: String?): UserPreferences {
        return userId?.let(preferencesByUser::get) ?: UserPreferences()
    }

    override suspend fun updatePreferences(userId: String, preferences: UserPreferences): Result<Unit> {
        preferencesByUser[userId] = preferences
        return Result.Success(Unit)
    }

    private fun HomeFeedCursor.toMarketplaceCursor() = MarketplacePageCursor(
        createdAt = createdAt,
        itemId = itemId.value
    )
}
