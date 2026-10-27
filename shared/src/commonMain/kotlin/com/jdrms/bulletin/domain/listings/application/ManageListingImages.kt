package com.jdrms.bulletin.domain.listings.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.repository.ListingImagesRepository

class ManageListingImages(
    private val repository: ListingImagesRepository
) {
    suspend fun synchronize(
        listingId: ListingId,
        sellerId: SellerId,
        retainedImageUrls: List<String>,
        newImages: List<ListingImageUpload>
    ): Result<List<String>> {
        if (retainedImageUrls.size + newImages.size > ListingImageUpload.MAX_LISTING_IMAGE_COUNT) {
            return Result.Error(IllegalArgumentException("A listing can have up to 5 images."))
        }
        return repository.synchronize(listingId, sellerId, retainedImageUrls, newImages)
    }

    suspend fun deleteAll(listingId: ListingId, sellerId: SellerId): Result<Unit> =
        repository.deleteAll(listingId, sellerId)
}
