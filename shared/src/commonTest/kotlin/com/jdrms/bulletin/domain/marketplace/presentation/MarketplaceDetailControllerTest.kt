package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.LoadMarketplaceListingDetail
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePrice
import com.jdrms.bulletin.domain.marketplace.domain.repository.MarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MarketplaceDetailControllerTest {
    private val item = MarketplaceItem(
        id = MarketplaceItemId("mkt_1"),
        sellerId = "seller_101",
        sellerName = "Cached Seller",
        title = "Cached title",
        description = "Cached description",
        price = MarketplacePrice(20.0),
        category = MarketplaceCategory.OTHER
    )
    private val state = MutableStateFlow(MarketplaceUiState(items = listOf(item)))
    private val response = CompletableDeferred<Result<Listing>>()
    private val repository = object : MarketplaceRepository by InMemoryMarketplaceRepository() {
        override suspend fun viewListing(listingID: String): Result<Listing> = response.await()
    }

    @Test
    fun cardDataIsAvailableSynchronouslyWhileFreshDetailsLoad() = runTest {
        val controller = MarketplaceDetailController(state, this, loader()) { null }
        controller.open("mkt_1")
        assertEquals(item.toDetailPreview(), state.value.detail.preview)
        assertNull(state.value.detail.listing)
        assertTrue(state.value.detail.isLoading)
        response.complete(InMemoryMarketplaceRepository().viewListing("mkt_1"))
        runCurrent()
        assertNull(state.value.detail.preview)
        assertEquals("mkt_1", state.value.detail.listing?.id?.value)
        assertFalse(state.value.detail.isLoading)
    }

    @Test
    fun dismissCancelsTheRequestAndDoesNotReopenDetails() = runTest {
        val controller = MarketplaceDetailController(state, this, loader()) { null }
        controller.open("mkt_1")
        runCurrent()
        controller.dismiss()
        response.complete(InMemoryMarketplaceRepository().viewListing("mkt_1"))
        runCurrent()
        assertEquals(MarketplaceDetailState(), state.value.detail)
    }

    @Test
    fun switchingSelectionDropsTheOldPreviewAndResponse() = runTest {
        val controller = MarketplaceDetailController(state, this, loader()) { null }
        controller.open("mkt_1")
        runCurrent()
        controller.open("missing")
        assertNull(state.value.detail.preview)
        controller.dismiss()
        runCurrent()
        assertNull(state.value.detail.listing)
    }

    @Test
    fun unavailableListingRemovesCachedContentAndOffersAnError() = runTest {
        val controller = MarketplaceDetailController(state, this, loader()) { null }
        controller.open("mkt_1")
        response.complete(Result.Error(IllegalStateException("This listing is unavailable.")))
        runCurrent()
        assertNull(state.value.detail.preview)
        assertNull(state.value.detail.listing)
        assertEquals("This listing is unavailable.", state.value.detail.errorMessage)
        assertFalse(state.value.detail.isLoading)
    }

    @Test
    fun externalPreviewIsAvailableWithoutTheListingInTheFeed() = runTest {
        state.value = MarketplaceUiState()
        val preview = item.toDetailPreview()
        val controller = MarketplaceDetailController(state, this, loader()) { null }
        controller.open(item.id.value, preview)
        assertEquals(preview, state.value.detail.preview)
        assertTrue(state.value.detail.isLoading)
        assertNull(state.value.detail.listing)
        response.complete(InMemoryMarketplaceRepository().viewListing(item.id.value))
        runCurrent()
        assertNull(state.value.detail.preview)
        assertEquals(item.id, state.value.detail.listing?.id)
        assertFalse(state.value.detail.isLoading)
    }

    private fun loader() = LoadMarketplaceListingDetail(ViewMarketplaceListing(repository))
}
