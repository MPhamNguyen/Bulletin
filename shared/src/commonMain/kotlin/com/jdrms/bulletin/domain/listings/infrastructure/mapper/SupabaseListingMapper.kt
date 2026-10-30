package com.jdrms.bulletin.domain.listings.infrastructure.mapper

import com.jdrms.bulletin.core.common.ImageCropSettings
import com.jdrms.bulletin.core.common.withCropSettings
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
    private val attributesCodec = ListingAttributesJsonCodec()

    /** Returns null when required fields are missing or the stored attributes payload is malformed. */
    fun toDomain(dto: SupabaseListingDto, imageUrl: (String) -> String = { it }): Listing? {
        val sellerId = dto.userId
        val title = dto.name
        if (sellerId.isNullOrBlank() || title.isNullOrBlank()) return null
        val price = dto.price
        if (price == null || !price.isFinite()) return null
        val category = runCatching { ListingCategory.valueOf(dto.category.orEmpty().uppercase()) }
            .getOrDefault(ListingCategory.OTHER)
        val attributes = (attributesCodec.decode(dto.attributes, category) as? ListingAttributesDecoding.Decoded)
            ?.attributes ?: return null

        return Listing(
            id = ListingId(dto.id),
            sellerId = SellerId(sellerId),
            buyerId = dto.buyerId,
            sellerName = dto.profile?.fullName?.takeIf(String::isNotBlank) ?: DEFAULT_SELLER_NAME,
            title = title,
            description = dto.description?.takeIf(String::isNotBlank) ?: DEFAULT_DESCRIPTION,
            price = ListingPrice(price.coerceAtLeast(0.0)),
            category = category,
            condition = runCatching { ListingCondition.valueOf(dto.condition.orEmpty().uppercase()) }
                .getOrDefault(ListingCondition.GOOD),
            status = dto.toListingStatus(),
            images = dto.listingImages.sortedBy { it.position }.map { image ->
                imageUrl(image.objectPath).withCropSettings(
                    ImageCropSettings(image.cropScale, image.cropOffsetX, image.cropOffsetY)
                )
            },
            createdAtMillis = dto.createdAt.toEpochMillisecondsOrZero(),
            updatedAtMillis = dto.updatedAt.toEpochMillisecondsOrZero(),
            attributes = attributes
        )
    }

    fun toInsertDto(listing: Listing, sellerId: String): SupabaseListingWriteDto {
        return SupabaseListingWriteDto(
            id = listing.id.value,
            name = listing.title,
            userId = sellerId,
            buyerId = listing.buyerId,
            category = listing.category.name,
            condition = listing.condition.name,
            price = listing.price.amount,
            description = listing.description,
            isSold = listing.status == ListingStatus.SOLD,
            attributes = attributesCodec.encode(listing.attributes)
        )
    }

    fun toInsertDto(listing: Listing): SupabaseListingWriteDto =
        toInsertDto(listing, listing.sellerId.value)

    private fun String?.toEpochMillisecondsOrZero(): Long {
        return this?.let { timestamp ->
            runCatching { Instant.parse(timestamp).toEpochMilliseconds() }.getOrNull()
        } ?: 0L
    }

    private fun SupabaseListingDto.toListingStatus(): ListingStatus {
        if (isSold) return ListingStatus.SOLD
        return runCatching { ListingStatus.valueOf(status.orEmpty().uppercase()) }
            .getOrDefault(ListingStatus.AVAILABLE)
    }

    private const val DEFAULT_SELLER_NAME = "Student"
    private const val DEFAULT_DESCRIPTION = "No description provided."
}
