package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.domain.listings.attributes.attributeListing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload
import com.jdrms.bulletin.domain.listings.domain.service.ListingPhotoPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ListingFormValidationTest {
    private val upload = ListingImageUpload(byteArrayOf(1), "image/jpeg")

    private fun state(
        title: String = "Desk lamp",
        description: String = "Works well",
        price: String = "500",
        photos: Int = 1
    ) = ListingsUiState(
        newTitle = title,
        newDescription = description,
        newPriceCents = price,
        newCategory = ListingCategory.OTHER,
        newImages = List(photos) { upload },
        editingListing = attributeListing(),
        editTitle = title,
        editDescription = description,
        editPriceCents = price,
        editCategory = ListingCategory.OTHER,
        editPhotos = List(photos) { ListingPhotoDraft.Pending(upload) }
    )

    @Test
    fun createAndEditAcceptTheSameCompleteForm() {
        val state = state()
        assertNotNull(validateNewListingDraft(state).draft)
        assertNotNull(validateEditListingDraft(state).draft)
    }

    @Test
    fun createAndEditReportTheSameErrorForEachMissingRequiredField() {
        listOf(
            state(price = "") to "Please enter a price",
            state(title = "ab") to "Title must be at least $MIN_LISTING_TITLE_LENGTH characters",
            state(description = "  ") to "Description cannot be empty",
            state(photos = 0) to ListingPhotoPolicy.REQUIRED_PHOTO_MESSAGE
        ).forEach { (invalid, message) ->
            val create = validateNewListingDraft(invalid)
            val edit = validateEditListingDraft(invalid)
            assertNull(create.draft)
            assertNull(edit.draft)
            assertEquals(message, create.errorMessage)
            assertEquals(message, edit.errorMessage)
        }
    }
}
