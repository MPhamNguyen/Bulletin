package com.jdrms.bulletin.domain.listings.infrastructure.repository

import com.jdrms.bulletin.core.common.ImageCropSettings
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.cropSettings
import com.jdrms.bulletin.core.common.generateUuid
import com.jdrms.bulletin.core.common.withCropSettings
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.repository.ListingImagesRepository

class InMemoryListingImagesRepository : ListingImagesRepository {
    private val imagesByListing = mutableMapOf<ListingId, OwnedListingImages>()

    override suspend fun synchronize(
        listingId: ListingId,
        sellerId: SellerId,
        retainedImageUrls: List<String>,
        newImages: List<ListingImageUpload>
    ): Result<List<String>> {
        val existing = imagesByListing[listingId]
        if (existing != null && existing.sellerId != sellerId) {
            return Result.Error(IllegalAccessException("Only the listing owner can manage its images."))
        }
        val current = existing?.urls.orEmpty()
        val retained = retainedImageUrls.mapNotNull { url ->
            current.find { it.substringBefore("#bulletin_crop=") == url.substringBefore("#bulletin_crop=") }
                ?.substringBefore("#bulletin_crop=")?.withCropSettings(url.cropSettings())
        }
        val uniqueRetainedImages = retainedImageUrls.map { it.substringBefore("#bulletin_crop=") }.distinct()
        if (retained.size != uniqueRetainedImages.size || retained.size + newImages.size > 5) {
            return Result.Error(IllegalArgumentException("Choose up to 5 available listing images."))
        }
        val added = newImages.map { image ->
            "memory://listing-image/${listingId.value}/${generateUuid()}".withCropSettings(
                ImageCropSettings(image.cropScale, image.cropOffsetX, image.cropOffsetY)
            )
        }
        val result = retained + added
        imagesByListing[listingId] = OwnedListingImages(sellerId, result)
        return Result.Success(result)
    }

    override suspend fun deleteAll(listingId: ListingId, sellerId: SellerId): Result<Unit> {
        val existing = imagesByListing[listingId]
        if (existing != null && existing.sellerId != sellerId) {
            return Result.Error(IllegalAccessException("Only the listing owner can manage its images."))
        }
        imagesByListing.remove(listingId)
        return Result.Success(Unit)
    }

    private data class OwnedListingImages(val sellerId: SellerId, val urls: List<String>)
}
