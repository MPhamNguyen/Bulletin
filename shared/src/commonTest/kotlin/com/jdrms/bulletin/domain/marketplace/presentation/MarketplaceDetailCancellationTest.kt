package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.LoadMarketplaceListingDetail
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.repository.MarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.MarketplaceReadCache
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MarketplaceDetailCancellationTest {
    @Test
    fun retryWaitsForCanceledCacheLoadCleanup() = verifyReplacement { it.retry() }

    @Test
    fun reopenWaitsForCanceledCacheLoadCleanup() = verifyReplacement {
        it.dismiss()
        it.open("mkt_1")
    }

    @Test
    fun repeatedRetriesPreserveTheCleanupChain() = verifyReplacement {
        it.retry()
        runCurrent()
        it.retry()
    }

    private fun verifyReplacement(
        replace: suspend TestScope.(MarketplaceDetailController) -> Unit
    ) = runTest {
        val repository = DelayedCancellationRepository()
        val state = MutableStateFlow(MarketplaceUiState())
        val load = LoadMarketplaceListingDetail(ViewMarketplaceListing(repository))
        val controller = MarketplaceDetailController(state, this, load) { null }
        controller.open("mkt_1")
        runCurrent()
        replace(controller)
        runCurrent()
        assertEquals(1, repository.loads)
        assertTrue(state.value.detail.isLoading)
        repository.cleanup.complete(Unit)
        runCurrent()
        assertEquals(2, repository.loads)
        assertEquals("mkt_1", state.value.detail.listing?.id?.value)
        assertFalse(state.value.detail.isLoading)
        assertNull(state.value.detail.errorMessage)
    }

    private class DelayedCancellationRepository(
        private val backing: MarketplaceRepository = InMemoryMarketplaceRepository()
    ) : MarketplaceRepository by backing {
        private val cache = MarketplaceReadCache()
        val cleanup = CompletableDeferred<Unit>()
        var loads = 0

        override suspend fun viewListing(listingID: String): Result<Listing> = Result.Success(
            cache.detail(listingID) {
                loads++
                if (loads == 1) {
                    try {
                        awaitCancellation()
                    } finally {
                        withContext(NonCancellable) { cleanup.await() }
                    }
                }
                (backing.viewListing(listingID) as Result.Success).data
            }
        )
    }
}
