package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.CreateListing
import com.jdrms.bulletin.domain.listings.application.CurrentListingSellerProvider
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ListingCreationConcurrencyTest {
    @Test
    fun olderFailureCannotRestoreOverANewerPendingSubmission() = overlappingFailures(olderFailsFirst = true)

    @Test
    fun olderFailureCannotReplaceTheNewerRestoredDraft() = overlappingFailures(olderFailsFirst = false)

    private fun overlappingFailures(olderFailsFirst: Boolean) = runTest {
        val firstSave = CompletableDeferred<Result<Listing>>()
        val secondSave = CompletableDeferred<Result<Listing>>()
        val stored = InMemoryListingsRepository(initialListings = emptyList())
        val repository = object : ListingsRepository by stored {
            override suspend fun createListing(listing: Listing): Result<Listing> =
                if (listing.title == "First draft") firstSave.await() else secondSave.await()
        }
        val state = MutableStateFlow(draft("First draft", "100"))
        val actions = ListingCreationActions(
            state = state,
            scope = this,
            createListing = CreateListing(repository),
            manageListing = ManageListing(repository),
            manageImages = null,
            sellerProvider = sellerProvider(),
            callbacks = ListingActionCallbacks(null, {}, {}),
            attributeActions = ListingAttributeDraftActions(state)
        )
        assertTrue(actions.submit())
        runCurrent()
        val secondDraft = draft("Second draft", "200")
        state.value = secondDraft.copy(myListings = state.value.myListings)
        assertTrue(actions.submit())
        runCurrent()

        val failure = Result.Error(IllegalStateException("Network down"))
        if (olderFailsFirst) {
            firstSave.complete(failure)
            runCurrent()
            assertEquals("", state.value.newTitle)
            assertTrue(state.value.newImages.isEmpty())
            secondSave.complete(failure)
        } else {
            secondSave.complete(failure)
            runCurrent()
            assertEquals(secondDraft.newTitle, state.value.newTitle)
            firstSave.complete(failure)
        }
        runCurrent()

        assertRestoredDraft(secondDraft, state.value)
    }

    private fun assertRestoredDraft(expected: ListingsUiState, actual: ListingsUiState) {
        assertTrue(actual.myListings.isEmpty())
        assertEquals(expected.newTitle, actual.newTitle)
        assertEquals(expected.newDescription, actual.newDescription)
        assertEquals(expected.newPriceCents, actual.newPriceCents)
        assertEquals(expected.newImages, actual.newImages)
        assertEquals(expected.newAttributes, actual.newAttributes)
    }

    private fun sellerProvider() = object : CurrentListingSellerProvider {
        override suspend fun getCurrentSeller() = Result.Success(ListingSeller(SellerId("seller"), "Seller"))
    }

    private fun draft(title: String, price: String) = ListingsUiState(
        newTitle = title,
        newDescription = "$title description",
        newPriceCents = price,
        newImages = listOf(ListingImageUpload(byteArrayOf(1), "image/jpeg"))
    )
}
