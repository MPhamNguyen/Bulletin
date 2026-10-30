package com.jdrms.bulletin.domain.listings.attributes

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.CreateListing
import com.jdrms.bulletin.domain.listings.application.CurrentListingSellerProvider
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import com.jdrms.bulletin.domain.listings.presentation.ListingActionCallbacks
import com.jdrms.bulletin.domain.listings.presentation.ListingAttributeDraft
import com.jdrms.bulletin.domain.listings.presentation.ListingAttributeDraftActions
import com.jdrms.bulletin.domain.listings.presentation.ListingCreationActions
import com.jdrms.bulletin.domain.listings.presentation.ListingEditActions
import com.jdrms.bulletin.domain.listings.presentation.ListingsUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ListingAttributeSubmissionTest {
    private val listing = attributeListing()
    private val seller = object : CurrentListingSellerProvider {
        override suspend fun getCurrentSeller() = Result.Success(ListingSeller(listing.sellerId, listing.sellerName))
    }
    private val callbacks = ListingActionCallbacks(null, {}, {})

    @Test
    fun createPersistsAttributesAndResetsDraftAfterSuccess() = runTest {
        val repository = InMemoryListingsRepository(emptyList())
        val state = MutableStateFlow(
            ListingsUiState(
                newTitle = listing.title,
                newDescription = listing.description,
                newPriceCents = "100",
                newCategory = listing.category,
                newAttributes = ListingAttributeDraft(inputs = mapOf("brand" to "Framework", "device_type" to "laptop"))
            )
        )
        ListingCreationActions(
            state,
            this,
            CreateListing(repository),
            ManageListing(repository),
            null,
            seller,
            callbacks,
            ListingAttributeDraftActions(state)
        ).submit()
        advanceUntilIdle()
        assertEquals(listing.attributes["brand"], repository.getAllListings().single().attributes["brand"])
        assertEquals(ListingAttributeDraft(), state.value.newAttributes)
    }

    @Test
    fun editLoadsSavesAndResetsAttributesAndCancelClearsDraft() = runTest {
        val repository = InMemoryListingsRepository(emptyList())
        repository.createListing(listing)
        val state = MutableStateFlow(ListingsUiState())
        val drafts = ListingAttributeDraftActions(state)
        val edit = ListingEditActions(state, this, ManageListing(repository), null, seller, callbacks, drafts)
        edit.start(listing)
        advanceUntilIdle()
        assertEquals("16", state.value.editAttributes.inputs["ram_gb"])
        drafts.inputChanged(com.jdrms.bulletin.domain.listings.presentation.ListingForm.EDIT, "ram_gb", "32")
        edit.save()
        advanceUntilIdle()
        assertEquals(
            "32",
            repository.getListing(listing.id)!!.attributes["ram_gb"].let {
                (it as com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeValue.Integer).value.toString()
            }
        )
        assertNull(state.value.editingListing)
        assertTrue(state.value.editAttributes.inputs.isEmpty())
        edit.start(repository.getListing(listing.id)!!)
        advanceUntilIdle()
        edit.cancel()
        assertEquals(ListingAttributeDraft(), state.value.editAttributes)
    }
}
