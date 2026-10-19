package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceReportIdentity
import com.jdrms.bulletin.domain.marketplace.application.ReportMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingReport
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId
import com.jdrms.bulletin.domain.marketplace.domain.repository.ListingReportRepository
import com.jdrms.bulletin.domain.reporting.domain.model.ReportReason
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ListingReportViewModelTest {
    @Test
    fun reportFlowDismissalCompletesOnlyAfterSubmission() {
        var formDismissed = false
        var reportDone = false

        handleMarketplaceListingReportDismissal(
            ListingReportUiState(submitted = false),
            dismissForm = { formDismissed = true },
            onReportDone = { reportDone = true }
        )

        assertTrue(formDismissed)
        assertFalse(reportDone)

        formDismissed = false
        handleMarketplaceListingReportDismissal(
            ListingReportUiState(submitted = true),
            dismissForm = { formDismissed = true },
            onReportDone = { reportDone = true }
        )

        assertFalse(formDismissed)
        assertTrue(reportDone)
    }

    @Test
    fun submitsSelectedReasonAndDetailsForTheListing() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var submitted: ListingReport? = null
            val useCase = ReportMarketplaceListing(
                MarketplaceReportIdentity { Result.Success(MarketplaceReporterId("reporter")) },
                repository { submitted = it }
            )
            val viewModel = ListingReportViewModel(useCase)
            viewModel.begin("listing:1", "seller")
            viewModel.selectReason(ReportReason.SCAM_OR_FRAUD)
            viewModel.updateDescription("Details")
            viewModel.submit()
            advanceUntilIdle()

            assertEquals("listing:1", submitted?.listingId?.value)
            assertEquals("reporter", submitted?.reporterId?.value)
            assertEquals(ReportReason.SCAM_OR_FRAUD, submitted?.reason)
            assertEquals("Details", submitted?.description)
            assertTrue(viewModel.uiState.value.submitted)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun requiresAReasonBeforeSubmission() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val useCase = ReportMarketplaceListing(
                MarketplaceReportIdentity { Result.Success(MarketplaceReporterId("reporter")) },
                repository { error("Unexpected report") }
            )
            val viewModel = ListingReportViewModel(useCase)
            viewModel.begin("listing:1", "seller")
            viewModel.submit()
            advanceUntilIdle()

            assertEquals("Choose a reason for your report.", viewModel.uiState.value.errorMessage)
            assertFalse(viewModel.uiState.value.isSubmitting)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun dismissClearsFormState() {
        val viewModel = ListingReportViewModel(
            ReportMarketplaceListing(
                MarketplaceReportIdentity { Result.Success(MarketplaceReporterId("reporter")) },
                repository {}
            )
        )
        viewModel.begin("listing:1", "seller")
        viewModel.selectReason(ReportReason.OTHER)

        viewModel.dismiss()

        assertEquals(ListingReportUiState(), viewModel.uiState.value)
    }

    private fun repository(onSubmit: (ListingReport) -> Unit) = object : ListingReportRepository {
        override suspend fun submit(report: ListingReport): Result<Unit> {
            onSubmit(report)
            return Result.Success(Unit)
        }

        override suspend fun hasOpenReport(
            reporterId: MarketplaceReporterId,
            listingId: MarketplaceItemId
        ): Result<Boolean> = Result.Success(false)

        override suspend fun reportedListingIds(
            reporterId: MarketplaceReporterId
        ): Result<Set<MarketplaceItemId>> = Result.Success(emptySet())
    }
}
