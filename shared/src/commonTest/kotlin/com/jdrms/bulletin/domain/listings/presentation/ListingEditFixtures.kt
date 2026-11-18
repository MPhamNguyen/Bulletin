package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload
import com.jdrms.bulletin.domain.listings.domain.model.ListingPrice
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingImagesRepository
import kotlin.test.assertIs

internal suspend fun editableTestListing(
    ownerId: SellerId,
    sellerName: String,
    images: InMemoryListingImagesRepository
): Listing = listingWithStoredPhoto(
    Listing(
        id = ListingId("list_test_vm"),
        sellerId = ownerId,
        sellerName = sellerName,
        title = "Tactile Keyboard",
        description = "Quiet tactile switches for studying",
        price = ListingPrice(40.0),
        category = ListingCategory.ELECTRONICS
    ),
    images
)

internal suspend fun listingWithStoredPhoto(listing: Listing, images: InMemoryListingImagesRepository): Listing {
    val result = images.synchronize(
        listing.id,
        listing.sellerId,
        emptyList(),
        listOf(ListingImageUpload(byteArrayOf(1), "image/jpeg"))
    )
    return listing.copy(images = assertIs<Result.Success<List<String>>>(result).data)
}
