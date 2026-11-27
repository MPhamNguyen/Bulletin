package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.domain.listings.domain.model.ListingUpdateIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ListingEditActionsTest {
    @Test
    fun failedImageListSaveRestoresEditorAfterOptimisticConfirmation() = runTest {
        val fixture = ListingEditTestFixture(this, failImageSave = true)
        fixture.repository.createListing(fixture.listing)
        fixture.edit.start(fixture.listing)
        advanceUntilIdle()
        fixture.state.value = fixture.state.value.copy(editTitle = "Saved details")
        fixture.addPhotoAndSave()
        advanceUntilIdle()

        assertEquals(2, fixture.repository.updateCalls)
        assertEquals("Saved details", fixture.state.value.editingListing?.title)
        assertEquals("Saved details", fixture.repository.getListing(fixture.listing.id)?.title)
        assertNull(fixture.state.value.successMessage)
        assertEquals(1, fixture.state.value.editPhotos.size)
        assertFalse(fixture.state.value.isUpdating)
        assertEquals(
            "Listing details were saved, but image changes failed: Image list save failed",
            fixture.state.value.errorMessage
        )
        assertEquals(listOf("Listing updated successfully!"), fixture.notifications)
        assertEquals(1, fixture.reloads)
    }

    @Test
    fun successfulImageListSaveClosesEditorAndRunsSuccessCallbacks() = runTest {
        val fixture = ListingEditTestFixture(this, failImageSave = false)
        fixture.repository.createListing(fixture.listing)
        fixture.edit.start(fixture.listing)
        advanceUntilIdle()
        fixture.addPhotoAndSave()
        advanceUntilIdle()

        assertEquals(2, fixture.repository.updateCalls)
        assertEquals(listOf(ListingUpdateIntent.EDIT, ListingUpdateIntent.ROUTINE), fixture.repository.intents)
        assertNull(fixture.state.value.editingListing)
        assertFalse(fixture.state.value.isUpdating)
        assertNull(fixture.state.value.errorMessage)
        assertEquals(listOf("Listing updated successfully!"), fixture.notifications)
        assertEquals(1, fixture.reloads)
    }
}
