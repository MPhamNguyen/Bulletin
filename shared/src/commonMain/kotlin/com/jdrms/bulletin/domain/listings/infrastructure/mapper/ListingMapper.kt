package com.jdrms.bulletin.domain.listings.infrastructure.mapper

import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingCondition
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingPrice
import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.infrastructure.dto.ListingDto

object ListingMapper {
    fun toDomain(dto: ListingDto): Listing {
        val category = runCatching { ListingCategory.valueOf(dto.category.uppercase()) }
            .getOrDefault(ListingCategory.OTHER)
        val condition = runCatching { ListingCondition.valueOf(dto.condition.uppercase()) }
            .getOrDefault(ListingCondition.GOOD)
        val status = runCatching { ListingStatus.valueOf(dto.status.uppercase()) }
            .getOrDefault(ListingStatus.AVAILABLE)

        return Listing(
            id = ListingId(dto.id),
            sellerId = SellerId(dto.sellerId),
            sellerName = dto.sellerName,
            title = dto.title,
            description = dto.description,
            price = ListingPrice(dto.price.coerceAtLeast(0.0)),
            category = category,
            condition = condition,
            status = status,
            createdAtMillis = dto.createdAtMillis
        )
    }

    fun toDto(domain: Listing): ListingDto {
        return ListingDto(
            id = domain.id.value,
            sellerId = domain.sellerId.value,
            sellerName = domain.sellerName,
            title = domain.title,
            description = domain.description,
            price = domain.price.amount,
            category = domain.category.name,
            condition = domain.condition.name,
            status = domain.status.name,
            createdAtMillis = domain.createdAtMillis
        )
    }
}
