package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.application.ReportedListingVisibilityPreferenceStore
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReportedListingVisibilityPreferenceViewModelTest {
    @Test
    fun readFailureKeepsSafeDefault() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val store = TestPreferenceStore().apply {
                readFailure = IllegalStateException("Storage unavailable")
            }
            val viewModel = ReportedListingVisibilityPreferenceViewModel(store)

            viewModel.setAccount(reporterId)
            advanceUntilIdle()

            assertTrue(viewModel.shouldHideReportedListings.value)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun failedSaveRestoresPersistedValueAndDoesNotCallSavedCallback() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val store = TestPreferenceStore(preference = false)
            val viewModel = ReportedListingVisibilityPreferenceViewModel(store)
            var savedCallbackCount = 0
            viewModel.setAccount(reporterId)
            advanceUntilIdle()

            store.writeFailure = IllegalStateException("Storage unavailable")
            viewModel.setHideReportedListings(true) { savedCallbackCount++ }
            advanceUntilIdle()

            assertFalse(viewModel.shouldHideReportedListings.value)
            assertEquals(0, savedCallbackCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun delayedReadDoesNotOverwriteNewerSwitchChoice() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val readResult = CompletableDeferred<Boolean>()
            val store = TestPreferenceStore(readResult = readResult)
            val viewModel = ReportedListingVisibilityPreferenceViewModel(store)
            var savedCallbackCount = 0
            viewModel.setAccount(reporterId)
            runCurrent()

            viewModel.setHideReportedListings(false) { savedCallbackCount++ }
            runCurrent()
            readResult.complete(true)
            advanceUntilIdle()

            assertFalse(viewModel.shouldHideReportedListings.value)
            assertFalse(store.preference)
            assertEquals(1, savedCallbackCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class TestPreferenceStore(
        var preference: Boolean = true,
        private val readResult: CompletableDeferred<Boolean>? = null
    ) : ReportedListingVisibilityPreferenceStore {
        var readFailure: Exception? = null
        var writeFailure: Exception? = null

        override suspend fun shouldHideReportedListings(reporterId: MarketplaceReporterId): Boolean {
            readFailure?.let { throw it }
            return readResult?.await() ?: preference
        }

        override suspend fun setHideReportedListings(
            reporterId: MarketplaceReporterId,
            shouldHide: Boolean
        ) {
            writeFailure?.let { throw it }
            preference = shouldHide
        }
    }

    private companion object {
        val reporterId = MarketplaceReporterId("reporter")
    }
}
