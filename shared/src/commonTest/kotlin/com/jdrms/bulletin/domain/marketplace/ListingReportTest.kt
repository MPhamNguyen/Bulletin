package com.jdrms.bulletin.domain.marketplace

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceReportIdentity
import com.jdrms.bulletin.domain.marketplace.application.ReportMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingReport
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingReportAlreadySubmittedException
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId
import com.jdrms.bulletin.domain.marketplace.domain.repository.ListingReportRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryListingReportRepository
import com.jdrms.bulletin.domain.reporting.domain.model.ReportReason
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ListingReportTest {
    @Test
    fun createsReportWithTrimmedOptionalDescription() {
        val result = ListingReport.create(
            MarketplaceItemId("listing-1"),
            "seller",
            MarketplaceReporterId("reporter"),
            ReportReason.SCAM_OR_FRAUD,
            "  Details  "
        )

        assertEquals("Details", assertIs<Result.Success<ListingReport>>(result).data.description)
    }

    @Test
    fun rejectsOwnListingAndDescriptionOverLimit() {
        val ownListing = ListingReport.create(
            MarketplaceItemId("listing-1"),
            "same",
            MarketplaceReporterId("same"),
            ReportReason.OTHER,
            ""
        )
        val longDescription = ListingReport.create(
            MarketplaceItemId("listing-1"),
            "seller",
            MarketplaceReporterId("reporter"),
            ReportReason.OTHER,
            "x".repeat(ListingReport.MAX_DESCRIPTION_LENGTH + 1)
        )

        assertIs<Result.Error>(ownListing)
        assertIs<Result.Error>(longDescription)
    }

    @Test
    fun reportSubmissionUsesAuthenticatedReporterAndRejectsMissingSession() = runTest {
        var submitted: ListingReport? = null
        val repository = object : ListingReportRepository {
            override suspend fun submit(report: ListingReport): Result<Unit> {
                submitted = report
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
        val authenticatedIdentity = MarketplaceReportIdentity { Result.Success(MarketplaceReporterId("reporter")) }
        val submit = ReportMarketplaceListing(authenticatedIdentity, repository)

        assertIs<Result.Success<Unit>>(
            submit(MarketplaceItemId("listing-1"), "seller", ReportReason.OTHER, "")
        )
        assertEquals("reporter", submitted?.reporterId?.value)

        val unauthenticated = ReportMarketplaceListing(
            MarketplaceReportIdentity { Result.Success(null) },
            repository
        )
        assertIs<Result.Error>(unauthenticated(MarketplaceItemId("listing-2"), "seller", ReportReason.OTHER, ""))
    }

    @Test
    fun repositoryContractPreventsDuplicateReportsByReporterAndListing() = runTest {
        val repository = InMemoryListingReportRepository()
        val first = report("listing-1", "reporter", ReportReason.OTHER)
        val duplicate = report("listing-1", "reporter", ReportReason.SCAM_OR_FRAUD)
        val sameListingOtherReporter = report("listing-1", "another", ReportReason.OTHER)

        assertIs<Result.Success<Unit>>(repository.submit(first))
        assertEquals(
            true,
            assertIs<Result.Success<Boolean>>(
                repository.hasOpenReport(first.reporterId, first.listingId)
            ).data
        )
        assertIs<ListingReportAlreadySubmittedException>(assertIs<Result.Error>(repository.submit(duplicate)).exception)
        assertIs<Result.Success<Unit>>(repository.submit(sameListingOtherReporter))
        assertEquals(listOf(first, sameListingOtherReporter), repository.submittedReports())
        assertEquals(
            setOf(MarketplaceItemId("listing-1")),
            assertIs<Result.Success<Set<MarketplaceItemId>>>(
                repository.reportedListingIds(MarketplaceReporterId("reporter"))
            ).data
        )
    }

    @Test
    fun allowsAnotherReportAfterEarlierReportIsClosedAndKeepsListingHidden() = runTest {
        val repository = InMemoryListingReportRepository()
        val closedReport = report("listing-1", "reporter", ReportReason.OTHER)
        val nextReport = report("listing-1", "reporter", ReportReason.SCAM_OR_FRAUD)
        assertIs<Result.Success<Unit>>(repository.submit(closedReport))
        assertEquals(
            true,
            assertIs<Result.Success<Boolean>>(
                repository.hasOpenReport(closedReport.reporterId, closedReport.listingId)
            ).data
        )

        repository.markClosedForTesting(closedReport)

        assertEquals(
            false,
            assertIs<Result.Success<Boolean>>(
                repository.hasOpenReport(closedReport.reporterId, closedReport.listingId)
            ).data
        )
        assertEquals(
            emptySet(),
            assertIs<Result.Success<Set<MarketplaceItemId>>>(
                repository.reportedListingIds(closedReport.reporterId)
            ).data
        )
        assertIs<Result.Success<Unit>>(repository.submit(nextReport))
        assertEquals(
            listOf(closedReport, nextReport),
            repository.submittedReports()
        )
        assertEquals(
            setOf(MarketplaceItemId("listing-1")),
            assertIs<Result.Success<Set<MarketplaceItemId>>>(
                repository.reportedListingIds(MarketplaceReporterId("reporter"))
            ).data
        )
    }

    @Test
    fun ownListingReportNeverReachesRepository() = runTest {
        var submitted: ListingReport? = null
        val repository = object : ListingReportRepository {
            override suspend fun submit(report: ListingReport): Result<Unit> {
                submitted = report
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
        val useCase = ReportMarketplaceListing(
            MarketplaceReportIdentity { Result.Success(MarketplaceReporterId("seller")) },
            repository
        )

        assertIs<Result.Error>(useCase(MarketplaceItemId("listing-1"), "seller", ReportReason.OTHER, ""))
        assertNull(submitted)
    }

    private fun report(listingId: String, reporterId: String, reason: ReportReason) =
        ListingReport.create(
            MarketplaceItemId(listingId),
            "seller",
            MarketplaceReporterId(reporterId),
            reason,
            ""
        ).getOrThrow()
}
