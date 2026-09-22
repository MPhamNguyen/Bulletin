package com.jdrms.bulletin.domain.listings.infrastructure.mapper

import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingCondition
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingPrice
import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingDto
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingWriteDto
import kotlin.time.Instant

object SupabaseListingMapper {
    fun toDomain(dto: SupabaseListingDto): Listing? {
        val sellerId = dto.userId?.takeIf(String::isNotBlank) ?: return null
        val title = dto.name?.takeIf(String::isNotBlank) ?: return null
        val price = dto.price ?: return null

        val category = runCatching { ListingCategory.valueOf(dto.category.orEmpty().uppercase()) }
            .getOrDefault(ListingCategory.OTHER)
        val condition = runCatching { ListingCondition.valueOf(dto.condition.orEmpty().uppercase()) }
            .getOrDefault(ListingCondition.GOOD)
        val status = runCatching { ListingStatus.valueOf(dto.status.orEmpty().uppercase()) }
            .getOrDefault(ListingStatus.AVAILABLE)

        return Listing(
            id = ListingId(dto.id),
            sellerId = SellerId(sellerId),
            sellerName = DEFAULT_SELLER_NAME,
            title = title,
            description = dto.description?.takeIf(String::isNotBlank) ?: DEFAULT_DESCRIPTION,
            price = ListingPrice(price.coerceAtLeast(0.0)),
            category = category,
            condition = condition,
            status = status,
            createdAtMillis = dto.createdAt.toEpochMillisecondsOrZero()
        )
    }

    fun toInsertDto(listing: Listing, sellerId: String): SupabaseListingWriteDto {
        return SupabaseListingWriteDto(
            id = listing.id.value,
            name = listing.title,
            userId = sellerId,
            category = listing.category.name,
            condition = listing.condition.name,
            price = listing.price.amount,
            description = listing.description
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
