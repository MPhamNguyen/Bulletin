package com.jdrms.bulletin.domain.listings.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
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
        val retained = current.filter(retainedImageUrls::contains)
        if (retained.size != retainedImageUrls.distinct().size || retained.size + newImages.size > 5) {
            return Result.Error(IllegalArgumentException("Choose up to 5 available listing images."))
        }
        val added = newImages.indices.map { index ->
            "memory://listing-image/${listingId.value}/${retained.size + index}"
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
