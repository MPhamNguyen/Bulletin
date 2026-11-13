package com.jdrms.bulletin.domain.marketplace.infrastructure.mapper

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterest
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePriceRange
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.MarketplaceInterestDto

internal object MarketplaceInterestMapper {
    fun toDomain(dto: MarketplaceInterestDto): MarketplaceInterest {
        val category = enumValue<MarketplaceCategory>("category", dto.category)
        val condition = dto.condition?.let { enumValue<MarketplaceCondition>("condition", it) }
        return MarketplaceInterest(
            category = category,
            condition = condition,
            priceRange = MarketplacePriceRange(dto.minimumPriceCents, dto.maximumPriceCents),
            attributes = MarketplaceInterestFilterJsonMapper.decode(dto.attributeFilters)
        )
    }

    fun toDto(interest: MarketplaceInterest) = MarketplaceInterestDto(
        category = interest.category.name,
        condition = interest.condition?.name,
        minimumPriceCents = interest.priceRange.minimumCents,
        maximumPriceCents = interest.priceRange.maximumCents,
        attributeFilters = MarketplaceInterestFilterJsonMapper.encode(interest.attributes)
    )

    private inline fun <reified T : Enum<T>> enumValue(label: String, value: String): T =
        runCatching { enumValueOf<T>(value) }.getOrElse { cause ->
            throw IllegalStateException("Unknown marketplace interest $label: $value", cause)
        }
}
