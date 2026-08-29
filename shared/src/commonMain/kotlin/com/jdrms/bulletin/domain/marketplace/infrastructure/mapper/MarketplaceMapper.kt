package com.jdrms.bulletin.domain.marketplace.infrastructure.mapper

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePrice
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.MarketplaceItemDto

object MarketplaceMapper {
    fun toDomain(dto: MarketplaceItemDto, isSaved: Boolean = false): MarketplaceItem {
        val category = runCatching { MarketplaceCategory.valueOf(dto.category.uppercase()) }
            .getOrDefault(MarketplaceCategory.OTHER)
        return MarketplaceItem(
            id = MarketplaceItemId(dto.id),
            sellerId = dto.sellerId,
            sellerName = dto.sellerName,
            title = dto.title,
            description = dto.description,
            price = MarketplacePrice(dto.price.coerceAtLeast(0.0)),
            category = category,
            isSaved = isSaved,
            createdAtMillis = dto.createdAtMillis
        )
    }

    fun toDto(domain: MarketplaceItem): MarketplaceItemDto {
        return MarketplaceItemDto(
            id = domain.id.value,
            sellerId = domain.sellerId,
            sellerName = domain.sellerName,
            title = domain.title,
            description = domain.description,
            price = domain.price.amount,
            category = domain.category.name,
            createdAtMillis = domain.createdAtMillis
        )
    }
}
