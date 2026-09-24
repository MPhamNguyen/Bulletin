package com.jdrms.bulletin.domain.marketplace.infrastructure.mapper

import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePrice
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.MarketplaceItemDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.MarketplaceListingDto

object MarketplaceMapper {
    fun toDomain(dto: MarketplaceItemDto, isSaved: Boolean = false): MarketplaceItem {
        val category = runCatching { MarketplaceCategory.valueOf(dto.category.uppercase()) }
            .getOrDefault(MarketplaceCategory.OTHER)
        return MarketplaceItem(
            id = MarketplaceItemId(dto.id),
            sellerId = dto.sellerId,
            sellerName = dto.sellerName.ifBlank { "Student Seller" },
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

    fun toListingDomain(
        dto: MarketplaceListingDto,
        isSaved: Boolean = false,
        reputationScore: Double? = null
    ): Listing {
        val category = runCatching { MarketplaceCategory.valueOf(dto.category.uppercase()) }
            .getOrDefault(MarketplaceCategory.OTHER)
        val score = (reputationScore ?: dto.reputationScore)?.coerceIn(0.0, 5.0)
        return Listing(
            id = MarketplaceItemId(dto.id),
            sellerId = dto.sellerId,
            sellerName = dto.sellerName.ifBlank { "Student Seller" },
            title = dto.title,
            description = dto.description.ifBlank { "No description provided." },
            price = MarketplacePrice(dto.price.coerceAtLeast(0.0)),
            category = category,
            condition = dto.condition,
            status = dto.status,
            photos = dto.photos,
            sellerReputationScore = score,
            isSaved = isSaved,
            createdAtMillis = dto.createdAtMillis
        )
    }

    fun toListingDto(domain: Listing): MarketplaceListingDto {
        return MarketplaceListingDto(
            id = domain.id.value,
            sellerId = domain.sellerId,
            sellerName = domain.sellerName,
            title = domain.title,
            description = domain.description,
            price = domain.price.amount,
            category = domain.category.name,
            condition = domain.condition,
            status = domain.status,
            photos = domain.photos,
            reputationScore = domain.sellerReputationScore,
            createdAtMillis = domain.createdAtMillis
        )
    }
}
