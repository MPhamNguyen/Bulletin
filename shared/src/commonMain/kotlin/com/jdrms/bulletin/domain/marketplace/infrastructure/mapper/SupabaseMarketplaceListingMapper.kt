package com.jdrms.bulletin.domain.marketplace.infrastructure.mapper

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSnapshot
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePrice
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.SupabaseMarketplaceListingDto
import kotlin.time.Instant

object SupabaseMarketplaceListingMapper {
    fun toListing(
        dto: SupabaseMarketplaceListingDto,
        isSaved: Boolean,
        reputationScore: Double?
    ): Listing? {
        val sellerId = dto.userId?.takeIf(String::isNotBlank) ?: return null
        val title = dto.name?.takeIf(String::isNotBlank) ?: return null
        val price = dto.price ?: return null
        val description = dto.description?.takeIf(String::isNotBlank) ?: DEFAULT_DESCRIPTION
        val category = dto.category.orEmpty().uppercase().ifBlank { "OTHER" }
        val condition = dto.condition.orEmpty().uppercase().ifBlank { "GOOD" }

        return Listing(
            id = MarketplaceItemId(dto.id),
            sellerId = sellerId,
            sellerName = DEFAULT_SELLER_NAME,
            title = title,
            description = description,
            price = MarketplacePrice(price.coerceAtLeast(0.0)),
            category = runCatching { MarketplaceCategory.valueOf(category) }
                .getOrDefault(MarketplaceCategory.OTHER),
            condition = condition,
            status = "AVAILABLE",
            sellerReputationScore = reputationScore,
            isSaved = isSaved,
            createdAtMillis = dto.createdAt.toEpochMillisecondsOrZero()
        )
    }

    fun toSnapshot(dto: SupabaseMarketplaceListingDto): MarketplaceListingSnapshot? {
        val sellerId = dto.userId?.takeIf(String::isNotBlank) ?: return null
        val title = dto.name?.takeIf(String::isNotBlank) ?: return null
        val price = dto.price ?: return null
        val category = runCatching { MarketplaceCategory.valueOf(dto.category.orEmpty().uppercase()) }
            .getOrDefault(MarketplaceCategory.OTHER)
        return MarketplaceListingSnapshot(
            id = "listing:${dto.id}",
            sellerId = sellerId,
            sellerName = DEFAULT_SELLER_NAME,
            title = title,
            description = dto.description.orEmpty(),
            priceAmount = price.coerceAtLeast(0.0),
            priceCurrency = "USD",
            category = category,
            createdAtMillis = dto.createdAt.toEpochMillisecondsOrZero()
        )
    }

    private fun String?.toEpochMillisecondsOrZero(): Long {
        return this?.let { timestamp ->
            runCatching { Instant.parse(timestamp).toEpochMilliseconds() }.getOrNull()
        } ?: 0L
    }

    private const val DEFAULT_SELLER_NAME = "Student"
    private const val DEFAULT_DESCRIPTION = "No description provided."
}
