package com.jdrms.bulletin.domain.listings.infrastructure.mapper

import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingDto
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingInsertDto
import kotlin.time.Instant

object SupabaseListingMapper {
    fun toDomain(dto: SupabaseListingDto): Listing {
        val domain = ListingMapper.toDomain(
            com.jdrms.bulletin.domain.listings.infrastructure.dto.ListingDto(
                id = dto.id,
                sellerId = dto.userId,
                sellerName = "Student",
                title = dto.name,
                description = dto.description,
                price = dto.price ?: 0.0,
                category = dto.category,
                condition = dto.condition,
                status = "AVAILABLE"
            )
        )
        return domain.copy(
            createdAtMillis = dto.createdAt?.let {
                runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrDefault(0L)
            } ?: 0L
        )
    }

    fun toInsertDto(listing: Listing): SupabaseListingInsertDto {
        return SupabaseListingInsertDto(
            id = listing.id.value,
            name = listing.title,
            userId = listing.sellerId.value,
            category = listing.category.name,
            condition = listing.condition.name,
            price = listing.price.amount,
            description = listing.description
        )
    }
}
