package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingCondition
import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload

sealed interface ListingPhotoDraft {
    data class Existing(val url: String) : ListingPhotoDraft
    data class Pending(val upload: ListingImageUpload) : ListingPhotoDraft
}

internal data class NewListingDraft(
    val title: String,
    val description: String,
    val price: Double,
    val category: ListingCategory,
    val condition: ListingCondition,
    val images: List<ListingImageUpload>
)

internal data class ValidatedEditDraft(
    val currentListing: Listing,
    val title: String,
    val description: String,
    val price: Double,
    val category: ListingCategory,
    val condition: ListingCondition,
    val photos: List<ListingPhotoDraft>
)
