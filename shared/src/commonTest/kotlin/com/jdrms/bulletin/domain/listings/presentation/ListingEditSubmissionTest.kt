package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.attributes.attributeListing
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ListingEditSubmissionTest {
    @Test
    fun closesEditorAndUpdatesCardAndToastBeforeTheSaveCompletes() = runTest {
        val gate = CompletableDeferred<Result<Listing>?>()
        val fixture = ListingEditTestFixture(this, beforeUpdate = { gate.await() })
        fixture.repository.createListing(fixture.listing)
        fixture.state.value = fixture.state.value.copy(myListings = listOf(fixture.listing))
        fixture.edit.start(fixture.listing)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(editTitle = "Updated laptop")
        fixture.addPhotoAndSave()

        assertNull(fixture.state.value.editingListing)
        assertEquals("Updated laptop", fixture.state.value.myListings.single().title)
        assertEquals(listOf("Listing updated successfully!"), fixture.notifications)
        runCurrent()
        assertEquals("Framework laptop", fixture.repository.getListing(fixture.listing.id)?.title)
        assertEquals(0, fixture.reloads)

        gate.complete(null)
        advanceUntilIdle()
        assertEquals("Updated laptop", fixture.repository.getListing(fixture.listing.id)?.title)
        assertFalse(fixture.state.value.isUpdating)
        assertNull(fixture.state.value.editingListing)
        assertEquals(1, fixture.notifications.size)
        assertEquals(1, fixture.reloads)
    }

    @Test
    fun failedSaveRollsBackCardAndRestoresEditedFieldsForRetry() = runTest {
        val gate = CompletableDeferred<Result<Listing>?>()
        val fixture = ListingEditTestFixture(this, beforeUpdate = { gate.await() })
        fixture.repository.createListing(fixture.listing)
        fixture.state.value = fixture.state.value.copy(myListings = listOf(fixture.listing))
        fixture.edit.start(fixture.listing)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(editTitle = "Unsaved title", editPriceCents = "4200")
        fixture.addPhotoAndSave()
        runCurrent()
        gate.complete(Result.Error(IllegalStateException("Offline")))
        advanceUntilIdle()

        val state = fixture.state.value
        assertEquals(fixture.listing, state.myListings.single())
        assertEquals(fixture.listing, state.editingListing)
        assertEquals("Unsaved title", state.editTitle)
        assertEquals("4200", state.editPriceCents)
        assertEquals(1, state.editPhotos.size)
        assertNotNull(state.errorMessage)
        assertNull(state.successMessage)
        assertFalse(state.isUpdating)
    }

    @Test
    fun completingASaveDoesNotCloseAnotherEditor() = runTest {
        val gate = CompletableDeferred<Result<Listing>?>()
        val fixture = ListingEditTestFixture(this, beforeUpdate = { gate.await() })
        fixture.repository.createListing(fixture.listing)
        fixture.edit.start(fixture.listing)
        runCurrent()
        fixture.addPhotoAndSave()
        runCurrent()
        val other = attributeListing(id = "another")
        fixture.edit.start(other)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(editTitle = "Another draft")
        gate.complete(null)
        advanceUntilIdle()

        assertEquals(other, fixture.state.value.editingListing)
        assertEquals("Another draft", fixture.state.value.editTitle)
    }

    @Test
    fun failingASaveDoesNotOverwriteAnotherEditor() = runTest {
        val gate = CompletableDeferred<Result<Listing>?>()
        val fixture = ListingEditTestFixture(this, beforeUpdate = { gate.await() })
        fixture.repository.createListing(fixture.listing)
        fixture.edit.start(fixture.listing)
        runCurrent()
        fixture.addPhotoAndSave()
        runCurrent()
        val other = attributeListing(id = "another")
        fixture.edit.start(other)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(editTitle = "Another draft")
        gate.complete(Result.Error(IllegalStateException("Offline")))
        advanceUntilIdle()

        assertEquals(other, fixture.state.value.editingListing)
        assertEquals("Another draft", fixture.state.value.editTitle)
        assertNotNull(fixture.state.value.errorMessage)
    }

    @Test
    fun failedDraftCanBeRetriedSuccessfully() = runTest {
        var fail = true
        val fixture = ListingEditTestFixture(this, beforeUpdate = {
            if (fail) Result.Error(IllegalStateException("Offline")) else null
        })
        fixture.repository.createListing(fixture.listing)
        fixture.edit.start(fixture.listing)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(editTitle = "Retried title")
        fixture.addPhotoAndSave()
        advanceUntilIdle()
        assertNotNull(fixture.state.value.editingListing)

        fail = false
        fixture.edit.save()
        advanceUntilIdle()
        assertNull(fixture.state.value.editingListing)
        assertNull(fixture.state.value.errorMessage)
        assertEquals("Retried title", fixture.repository.getListing(fixture.listing.id)?.title)
    }

    @Test
    fun cancellingAndReopeningDuringASaveCannotSubmitADuplicate() = runTest {
        val gate = CompletableDeferred<Result<Listing>?>()
        val fixture = ListingEditTestFixture(this, beforeUpdate = { gate.await() })
        fixture.repository.createListing(fixture.listing)
        fixture.edit.start(fixture.listing)
        runCurrent()
        fixture.addPhotoAndSave()
        runCurrent()
        fixture.edit.cancel()
        fixture.edit.start(fixture.listing)
        runCurrent()
        fixture.addPhotoAndSave()
        runCurrent()
        assertEquals(1, fixture.repository.updateCalls)

        gate.complete(null)
        advanceUntilIdle()
        assertEquals(2, fixture.repository.updateCalls)
        assertNotNull(fixture.state.value.editingListing)
    }

    @Test
    fun changedSessionCannotPersistAnOptimisticEditForThePreviousSeller() = runTest {
        val fixture = ListingEditTestFixture(this)
        fixture.repository.createListing(fixture.listing)
        fixture.edit.start(fixture.listing)
        runCurrent()
        fixture.sellerResult = Result.Success(ListingSeller(SellerId("other-seller"), "Other seller"))
        fixture.addPhotoAndSave()
        advanceUntilIdle()

        assertEquals(0, fixture.repository.updateCalls)
        assertEquals(fixture.listing, fixture.state.value.editingListing)
        assertNotNull(fixture.state.value.errorMessage)
        assertNull(fixture.state.value.successMessage)
    }

    @Test
    fun invalidDraftStaysOpenAndDoesNotShowConfirmationOrSave() = runTest {
        val fixture = ListingEditTestFixture(this)
        fixture.edit.start(fixture.listing)
        runCurrent()
        fixture.state.value = fixture.state.value.copy(editTitle = "No")
        fixture.edit.save()
        advanceUntilIdle()

        assertNotNull(fixture.state.value.editingListing)
        assertNotNull(fixture.state.value.errorMessage)
        assertTrue(fixture.notifications.isEmpty())
        assertEquals(0, fixture.repository.updateCalls)
    }
}
