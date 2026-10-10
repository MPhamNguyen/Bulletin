package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceUserReportReason
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceUserReporter
import com.jdrms.bulletin.domain.marketplace.application.ReportMarketplaceUser
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
class MarketplaceUserReportViewModelTest {
    @Test
    fun submitsSelectedReasonAndDescriptionForTheViewedSeller() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var submitted: Triple<String, MarketplaceUserReportReason, String>? = null
            val reporter = MarketplaceUserReporter { userId, reason, description ->
                submitted = Triple(userId, reason, description)
                Result.Success(Unit)
            }
            val viewModel = MarketplaceUserReportViewModel(ReportMarketplaceUser(reporter))
            viewModel.begin("seller-1")
            viewModel.selectReason(MarketplaceUserReportReason.SCAM_OR_FRAUD)
            viewModel.updateDescription("Details")
            viewModel.submit()
            advanceUntilIdle()

            assertEquals(Triple("seller-1", MarketplaceUserReportReason.SCAM_OR_FRAUD, "Details"), submitted)
            assertEquals(true, viewModel.uiState.value.submitted)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun requiresAReasonBeforeSubmission() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var called = false
            val reporter = MarketplaceUserReporter { _, _, _ ->
                called = true
                Result.Success(Unit)
            }
            val viewModel = MarketplaceUserReportViewModel(ReportMarketplaceUser(reporter))
            viewModel.begin("seller-1")
            viewModel.submit()
            advanceUntilIdle()

            assertFalse(called)
            assertEquals("Choose a reason for your report.", viewModel.uiState.value.errorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun showsSubmittingStateAndIgnoresRepeatedSubmitUntilFinished() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val response = CompletableDeferred<Result<Unit>>()
            var attempts = 0
            val reporter = MarketplaceUserReporter { _, _, _ ->
                attempts++
                response.await()
            }
            val viewModel = createReadyViewModel(reporter)

            viewModel.submit()
            runCurrent()
            assertTrue(viewModel.uiState.value.isSubmitting)
            viewModel.submit()
            runCurrent()
            assertEquals(1, attempts)

            response.complete(Result.Success(Unit))
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.submitted)
            assertFalse(viewModel.uiState.value.isSubmitting)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun displaysDuplicateErrorAndPreservesFormForRetryOrExit() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val duplicateMessage = "You have already reported this user."
            val reporter = MarketplaceUserReporter { _, _, _ ->
                Result.Error(IllegalStateException(duplicateMessage))
            }
            val viewModel = createReadyViewModel(reporter)

            viewModel.submit()
            advanceUntilIdle()

            assertEquals(duplicateMessage, viewModel.uiState.value.errorMessage)
            assertEquals(MarketplaceUserReportReason.SCAM_OR_FRAUD, viewModel.uiState.value.selectedReason)
            assertEquals("Details", viewModel.uiState.value.description)
            assertFalse(viewModel.uiState.value.isSubmitting)
            assertFalse(viewModel.uiState.value.submitted)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun displaysRetryGuidanceForUnexpectedSubmissionFailure() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val reporter = MarketplaceUserReporter { _, _, _ ->
                throw IllegalStateException("private backend detail")
            }
            val viewModel = createReadyViewModel(reporter)

            viewModel.submit()
            advanceUntilIdle()

            assertEquals(
                "We couldn't submit your report right now. Please try again.",
                viewModel.uiState.value.errorMessage
            )
            assertEquals("Details", viewModel.uiState.value.description)
            assertFalse(viewModel.uiState.value.isSubmitting)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createReadyViewModel(reporter: MarketplaceUserReporter): MarketplaceUserReportViewModel {
        return MarketplaceUserReportViewModel(ReportMarketplaceUser(reporter)).apply {
            begin("seller-1")
            selectReason(MarketplaceUserReportReason.SCAM_OR_FRAUD)
            updateDescription("Details")
        }
    }
}
