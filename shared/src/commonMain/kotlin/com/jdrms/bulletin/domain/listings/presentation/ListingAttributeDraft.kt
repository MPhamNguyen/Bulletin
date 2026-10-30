package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.service.DiscardedListingAttribute

/**
 * The seller's in-progress attribute form: raw text per stable key, inline [errors] per key, and the stored
 * entries this client cannot interpret ([preserved]) so a save returns them to storage untouched.
 */
data class ListingAttributeDraft(
    val inputs: Map<String, String> = emptyMap(),
    val errors: Map<String, String> = emptyMap(),
    val preserved: Map<String, String> = emptyMap()
)

enum class ListingForm { NEW, EDIT }

/** A category switch that would discard nonblank values; held until the seller confirms or cancels. */
data class PendingCategoryChange(
    val form: ListingForm,
    val target: ListingCategory,
    val discarded: List<DiscardedListingAttribute>
)
