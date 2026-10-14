package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.BlockMarketplaceUser
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceUserBlocker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MarketplaceUserBlockViewModelTest {
    @Test
    fun successfulBlockSetsBlockedState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var blockedUser: String? = null
            val viewModel = MarketplaceUserBlockViewModel(
                BlockMarketplaceUser(
                    MarketplaceUserBlocker { userId ->
                        blockedUser = userId
                        Result.Success(Unit)
                    }
                )
            )
            viewModel.begin("seller")
            viewModel.submit()
            advanceUntilIdle()

            assertEquals("seller", blockedUser)
            assertTrue(viewModel.uiState.value.blocked)
            assertFalse(viewModel.uiState.value.isBlocking)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun failedBlockExposesErrorAndDoesNotMarkUserBlocked() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = MarketplaceUserBlockViewModel(
                BlockMarketplaceUser(
                    MarketplaceUserBlocker {
                        throw IllegalStateException("not signed in")
                    }
                )
            )
            viewModel.begin("seller")
            viewModel.submit()
            advanceUntilIdle()

            assertEquals("Unable to block this user. Please try again.", viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.blocked)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
