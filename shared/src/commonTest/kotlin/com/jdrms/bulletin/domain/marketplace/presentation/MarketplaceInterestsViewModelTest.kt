package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceInterestAlert
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceInterestNotifier
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceInterestPreferenceStore
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSnapshot
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.application.pageFor
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterest
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterestOwnerId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class MarketplaceInterestsViewModelTest {
    @Test
    fun loadsAndPersistsSelectedCategoriesForTheCurrentOwner() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val store = TestInterestStore(
                mutableMapOf(ownerId to setOf(MarketplaceInterest(MarketplaceCategory.TEXTBOOKS)))
            )
            val viewModel = MarketplaceInterestsViewModel(store)

            viewModel.setOwner(ownerId)
            advanceUntilIdle()
            viewModel.toggle(MarketplaceCategory.ELECTRONICS)
            advanceUntilIdle()

            val expected = setOf(MarketplaceCategory.TEXTBOOKS, MarketplaceCategory.ELECTRONICS)
            assertEquals(expected, viewModel.uiState.value.selectedCategories)
            assertEquals(expected, store.values[ownerId]?.map { it.category }?.toSet())
            assertFalse(viewModel.uiState.value.isSaving)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun failedSaveRestoresPersistedCategoriesAndShowsAnError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val stored = setOf(MarketplaceInterest(MarketplaceCategory.FURNITURE))
            val store = TestInterestStore(mutableMapOf(ownerId to stored))
            val viewModel = MarketplaceInterestsViewModel(store)
            viewModel.setOwner(ownerId)
            advanceUntilIdle()
            store.writeFailure = IllegalStateException("Storage unavailable")

            viewModel.toggle(MarketplaceCategory.CLOTHING)
            advanceUntilIdle()

            assertEquals(stored.map { it.category }.toSet(), viewModel.uiState.value.selectedCategories)
            assertEquals("Couldn't save interests. Try again.", viewModel.uiState.value.errorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun updatesAndPersistsFiltersForASelectedCategory() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val initial = MarketplaceInterest(MarketplaceCategory.ELECTRONICS)
            val store = TestInterestStore(mutableMapOf(ownerId to setOf(initial)))
            val viewModel = MarketplaceInterestsViewModel(store)
            viewModel.setOwner(ownerId)
            advanceUntilIdle()

            val updated = initial.copy(condition = MarketplaceCondition.LIKE_NEW)
            viewModel.updateInterest(updated)
            advanceUntilIdle()

            assertEquals(updated, viewModel.uiState.value.interests[MarketplaceCategory.ELECTRONICS])
            assertEquals(setOf(updated), store.values[ownerId])
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun newMatchingListingPopulatesInterestsAndCreatesUnreadIndicator() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val fixture = matchFixture()
            val viewModel = fixture.viewModel

            viewModel.setOwner(ownerId)
            advanceUntilIdle()
            assertEquals(listOf("first"), viewModel.uiState.value.matchingListings.map { it.id.value })
            assertEquals(0, viewModel.uiState.value.unreadMatchCount)

            fixture.listings += snapshot("second", MarketplaceCategory.ELECTRONICS)
            fixture.listings += snapshot("ignored", MarketplaceCategory.CLOTHING)
            fixture.events.publish(MarketplaceInterestListingEvent(ownerId, MarketplaceItemId("second")))
            advanceUntilIdle()

            assertEquals(setOf("first", "second"), viewModel.uiState.value.matchingListings.map { it.id.value }.toSet())
            assertEquals(1, viewModel.uiState.value.unreadMatchCount)

            viewModel.setInterestsVisible(true)
            assertEquals(0, viewModel.uiState.value.unreadMatchCount)
            fixture.listings += snapshot("third", MarketplaceCategory.ELECTRONICS)
            fixture.events.publish(MarketplaceInterestListingEvent(ownerId, MarketplaceItemId("third")))
            advanceUntilIdle()
            assertEquals(0, viewModel.uiState.value.unreadMatchCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun matchFixture(): MatchFixture {
        val store = TestInterestStore(
            mutableMapOf(ownerId to setOf(MarketplaceInterest(MarketplaceCategory.ELECTRONICS)))
        )
        val events = MarketplaceInterestListingEvents()
        val listings = mutableListOf(snapshot("first", MarketplaceCategory.ELECTRONICS))
        val notifier = RecordingInterestNotifier()
        val source = MarketplaceListingSource { request -> listings.pageFor(request) }
        val search = SearchMarketplace(
            InMemoryMarketplaceRepository(initialListings = emptyList()),
            source
        )
        return MatchFixture(
            MarketplaceInterestsViewModel(store, search, events, notifier = notifier),
            events,
            listings,
            notifier
        )
    }

    private data class MatchFixture(
        val viewModel: MarketplaceInterestsViewModel,
        val events: MarketplaceInterestListingEvents,
        val listings: MutableList<MarketplaceListingSnapshot>,
        val notifier: RecordingInterestNotifier
    )

    private class TestInterestStore(
        val values: MutableMap<MarketplaceInterestOwnerId, Set<MarketplaceInterest>> = mutableMapOf()
    ) : MarketplaceInterestPreferenceStore {
        var writeFailure: Exception? = null

        override suspend fun get(ownerId: MarketplaceInterestOwnerId): Set<MarketplaceInterest> =
            values[ownerId].orEmpty()

        override suspend fun set(
            ownerId: MarketplaceInterestOwnerId,
            interests: Set<MarketplaceInterest>
        ) {
            writeFailure?.let { throw it }
            values[ownerId] = interests
        }
    }

    private class RecordingInterestNotifier : MarketplaceInterestNotifier {
        val alerts = mutableListOf<MarketplaceInterestAlert>()

        override fun show(alert: MarketplaceInterestAlert) {
            alerts += alert
        }

        override fun clearAll() = Unit
    }

    private companion object {
        val ownerId = MarketplaceInterestOwnerId("student-one")

        fun snapshot(id: String, category: MarketplaceCategory) = MarketplaceListingSnapshot(
            id = id,
            sellerId = "seller-$id",
            sellerName = "Seller",
            title = "Listing $id",
            description = "Description",
            priceAmount = 10.0,
            priceCurrency = "USD",
            category = category,
            createdAtMillis = if (id == "second") 2L else 1L
        )
    }
}
