package com.jdrms.bulletin.domain.marketplace

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceReportIdentity
import com.jdrms.bulletin.domain.marketplace.application.ReportMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingReport
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingReportAlreadySubmittedException
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingReportReason
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId
import com.jdrms.bulletin.domain.marketplace.domain.repository.ListingReportRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryListingReportRepository
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
            ListingReportReason.SCAM_OR_FRAUD,
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
            ListingReportReason.OTHER,
            ""
        )
        val longDescription = ListingReport.create(
            MarketplaceItemId("listing-1"),
            "seller",
            MarketplaceReporterId("reporter"),
            ListingReportReason.OTHER,
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

            override suspend fun reportedListingIds(
                reporterId: MarketplaceReporterId
            ): Result<Set<MarketplaceItemId>> = Result.Success(emptySet())
        }
        val authenticatedIdentity = MarketplaceReportIdentity { Result.Success(MarketplaceReporterId("reporter")) }
        val submit = ReportMarketplaceListing(authenticatedIdentity, repository)

        assertIs<Result.Success<Unit>>(
            submit(MarketplaceItemId("listing-1"), "seller", ListingReportReason.OTHER, "")
        )
        assertEquals("reporter", submitted?.reporterId?.value)

        val unauthenticated = ReportMarketplaceListing(
            MarketplaceReportIdentity { Result.Success(null) },
            repository
        )
        assertIs<Result.Error>(unauthenticated(MarketplaceItemId("listing-2"), "seller", ListingReportReason.OTHER, ""))
    }

    @Test
    fun repositoryContractPreventsDuplicateReportsByReporterAndListing() = runTest {
        val repository = InMemoryListingReportRepository()
        val first = report("listing-1", "reporter", ListingReportReason.OTHER)
        val duplicate = report("listing-1", "reporter", ListingReportReason.SCAM_OR_FRAUD)
        val sameListingOtherReporter = report("listing-1", "another", ListingReportReason.OTHER)

        assertIs<Result.Success<Unit>>(repository.submit(first))
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
    fun ownListingReportNeverReachesRepository() = runTest {
        var submitted: ListingReport? = null
        val repository = object : ListingReportRepository {
            override suspend fun submit(report: ListingReport): Result<Unit> {
                submitted = report
                return Result.Success(Unit)
            }

            override suspend fun reportedListingIds(
                reporterId: MarketplaceReporterId
            ): Result<Set<MarketplaceItemId>> = Result.Success(emptySet())
        }
        val useCase = ReportMarketplaceListing(
            MarketplaceReportIdentity { Result.Success(MarketplaceReporterId("seller")) },
            repository
        )

        assertIs<Result.Error>(useCase(MarketplaceItemId("listing-1"), "seller", ListingReportReason.OTHER, ""))
        assertNull(submitted)
    }

    private fun report(listingId: String, reporterId: String, reason: ListingReportReason) =
        ListingReport.create(
            MarketplaceItemId(listingId),
            "seller",
            MarketplaceReporterId(reporterId),
            reason,
            ""
        ).getOrThrow()
}
