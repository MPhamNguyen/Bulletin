package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterest
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterestOwnerId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId

internal class MarketplaceInterestMatchLoader(
    private val search: SearchMarketplace?,
    private val invalidateReads: suspend () -> Unit
) {
    suspend fun invalidate() = invalidateReads()

    suspend fun load(
        owner: MarketplaceInterestOwnerId,
        interests: Collection<MarketplaceInterest>
    ): List<MarketplaceItem>? = search?.let { source ->
        interests.flatMap { interest ->
            source.getPage(
                interest.toPageRequest(owner)
            ).items
        }.distinctBy { it.id }.sortedByDescending { it.createdAtMillis }
    }

    suspend fun getById(id: MarketplaceItemId): MarketplaceItem? = search?.getById(id)
}

private fun MarketplaceInterest.toPageRequest(owner: MarketplaceInterestOwnerId) =
    com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest(
        category = category,
        condition = condition,
        priceRange = priceRange,
        attributeFilters = attributes,
        viewerId = owner.value,
        pageSize = MATCH_PAGE_SIZE
    )

private const val MATCH_PAGE_SIZE = 20
