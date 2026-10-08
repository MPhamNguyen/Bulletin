package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingCondition

internal data class NewListingDraft(
    val title: String,
    val description: String,
    val price: Double,
    val category: ListingCategory,
    val condition: ListingCondition
)

internal data class ValidatedEditDraft(
    val currentListing: Listing,
    val title: String,
    val description: String,
    val price: Double,
    val category: ListingCategory,
    val condition: ListingCondition,
)
