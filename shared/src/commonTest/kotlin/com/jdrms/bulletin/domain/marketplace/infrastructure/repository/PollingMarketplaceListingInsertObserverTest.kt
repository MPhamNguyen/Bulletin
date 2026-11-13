package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.application.EmptyMarketplaceListingInsertObserver
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterestOwnerId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PollingMarketplaceListingInsertObserverTest {
    @Test
    fun emitsListingAddedAfterInitialSnapshot() = runTest {
        val initial = MarketplaceItemId("existing")
        val inserted = MarketplaceItemId("inserted")
        var snapshot = setOf(initial)
        val observer = PollingMarketplaceListingInsertObserver(
            realtime = EmptyMarketplaceListingInsertObserver,
            snapshots = MarketplaceListingIdSnapshotSource { snapshot },
            pollingIntervalMillis = 1_000L
        )

        val result = async {
            observer.observe(MarketplaceInterestOwnerId("owner")).first()
        }
        runCurrent()
        snapshot = setOf(inserted, initial)
        advanceTimeBy(1_000L)
        runCurrent()

        assertEquals(inserted, result.await())
    }
}
