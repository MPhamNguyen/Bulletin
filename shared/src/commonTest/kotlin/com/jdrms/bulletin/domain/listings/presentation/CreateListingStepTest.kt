package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CreateListingStepTest {
    private val photos = listOf(ListingImageUpload(byteArrayOf(1), "image/jpeg"))

    @Test
    fun photosRequiresThreeNonPaddingTitleCharacters() {
        assertFalse(CreateListingStep.PHOTOS.canContinue(ListingsUiState(newTitle = " ab ", newImages = photos)))
        assertTrue(CreateListingStep.PHOTOS.canContinue(ListingsUiState(newTitle = " abc ", newImages = photos)))
    }

    @Test
    fun removingTheLastPhotoBlocksBothPhotosAndSubmission() {
        val complete = ListingsUiState(
            newTitle = "Book",
            newPriceCents = "1250",
            newDescription = "Meet on campus",
            newImages = photos
        )
        assertTrue(CreateListingStep.PHOTOS.canContinue(complete))
        assertFalse(CreateListingStep.PHOTOS.canContinue(complete.copy(newImages = emptyList())))
        assertFalse(CreateListingStep.PRICE.canContinue(complete.copy(newImages = emptyList())))
    }

    @Test
    fun optionalDetailsDoNotBlockContinuation() {
        assertTrue(CreateListingStep.DETAILS.canContinue(ListingsUiState()))
    }

    @Test
    fun priceRequiresACompleteDraft() {
        val complete = ListingsUiState(
            newTitle = "Book",
            newPriceCents = "1250",
            newDescription = "Meet on campus",
            newImages = photos
        )
        assertTrue(CreateListingStep.PRICE.canContinue(complete))
        assertFalse(CreateListingStep.PRICE.canContinue(complete.copy(newPriceCents = "")))
        assertFalse(CreateListingStep.PRICE.canContinue(complete.copy(newDescription = " ")))
        assertFalse(CreateListingStep.PRICE.canContinue(complete.copy(newTitle = "ab")))
    }
}
