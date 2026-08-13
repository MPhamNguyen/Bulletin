package com.jdrms.bulletin.domain.marketplace.infrastructure.mapper

import com.jdrms.bulletin.domain.marketplace.domain.model.*
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.ListingDto

object ListingMapper {
    fun toDomain(dto: ListingDto): Listing {
        val category = try {
            Category.valueOf(dto.categoryName.uppercase())
        } catch (e: Exception) {
            Category.OTHER
        }
        val status = try {
            ListingStatus.valueOf(dto.statusCode.uppercase())
        } catch (e: Exception) {
            ListingStatus.AVAILABLE
        }

        return Listing(
            id = ListingId(dto.id),
            sellerId = SellerId(dto.sellerId),
            sellerName = dto.sellerName,
            title = dto.title,
            description = dto.description,
            price = Price(dto.priceAmount, dto.priceCurrency),
            category = category,
            status = status,
            imageUrl = dto.imageUrl,
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
            priceAmount = domain.price.amount,
            priceCurrency = domain.price.currency,
            categoryName = domain.category.name,
            statusCode = domain.status.name,
            imageUrl = domain.imageUrl,
            createdAtMillis = domain.createdAtMillis
        )
    }
}
