package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceInterestAlert
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceInterestIdentity
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceInterestNotifier
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingInsertObserver
import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterestOwnerId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.MarketplaceListingDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class MarketplaceInterestNotificationCoordinatorTest {
    @Test
    fun signedInSessionNotifiesForMatchingInsertWithoutOpeningInterests() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val owner = MarketplaceInterestOwnerId("student-one")
            val owners = MutableStateFlow<MarketplaceInterestOwnerId?>(owner)
            val inserts = MutableSharedFlow<MarketplaceItemId>(extraBufferCapacity = 1)
            val dto = dto("matching", MarketplaceCategory.ELECTRONICS)
            val notifier = RecordingNotifier()
            val listingEvents = MarketplaceInterestListingEvents()
            val search = SearchMarketplace(
                InMemoryMarketplaceRepository(initialListings = listOf(dto))
            )
            val coordinator = MarketplaceInterestNotificationCoordinator(
                identity = MarketplaceInterestIdentity { owners },
                listingObserver = MarketplaceListingInsertObserver { inserts },
                listingEvents = listingEvents,
                matchLoader = MarketplaceInterestMatchLoader(search) {},
                notifier = notifier
            )

            coordinator.start()
            advanceUntilIdle()
            inserts.emit(MarketplaceItemId("matching"))
            advanceUntilIdle()

            assertEquals(listOf("matching"), notifier.alerts.map { it.listingId.value })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun listingFoundByIdEvenWhenNotOnFirstPage() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val owner = MarketplaceInterestOwnerId("student-one")
            val owners = MutableStateFlow<MarketplaceInterestOwnerId?>(owner)
            val inserts = MutableSharedFlow<MarketplaceItemId>(extraBufferCapacity = 1)
            val dto = dto("eventually-visible", MarketplaceCategory.ELECTRONICS)
            val notifier = RecordingNotifier()
            val search = SearchMarketplace(
                InMemoryMarketplaceRepository(initialListings = listOf(dto))
            )
            val coordinator = MarketplaceInterestNotificationCoordinator(
                identity = MarketplaceInterestIdentity { owners },
                listingObserver = MarketplaceListingInsertObserver { inserts },
                listingEvents = MarketplaceInterestListingEvents(),
                matchLoader = MarketplaceInterestMatchLoader(search) {},
                notifier = notifier
            )

            coordinator.start()
            advanceUntilIdle()
            inserts.emit(MarketplaceItemId("eventually-visible"))
            advanceUntilIdle()

            assertEquals(listOf("eventually-visible"), notifier.alerts.map { it.listingId.value })
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class RecordingNotifier : MarketplaceInterestNotifier {
        val alerts = mutableListOf<MarketplaceInterestAlert>()
        override fun show(alert: MarketplaceInterestAlert) {
            alerts += alert
        }
        override fun clearAll() = Unit
    }

    private companion object {
        fun dto(id: String, category: MarketplaceCategory) = MarketplaceListingDto(
            id = id,
            sellerId = "seller-$id",
            sellerName = "Seller",
            title = "Listing $id",
            description = "Description",
            price = 10.0,
            category = category.name,
            condition = "UNKNOWN",
            status = "AVAILABLE",
            createdAtMillis = 1L
        )
    }
}
