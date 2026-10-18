package com.jdrms.bulletin.domain.listings.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload
import com.jdrms.bulletin.domain.listings.domain.model.SellerId

interface ListingImagesRepository {
    suspend fun synchronize(
        listingId: ListingId,
        sellerId: SellerId,
        retainedImageUrls: List<String>,
        newImages: List<ListingImageUpload>
    ): Result<List<String>>

    suspend fun deleteAll(listingId: ListingId, sellerId: SellerId): Result<Unit>
}
