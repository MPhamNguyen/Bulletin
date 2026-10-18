package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingReport
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingReportAlreadySubmittedException
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId
import com.jdrms.bulletin.domain.marketplace.domain.repository.ListingReportRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryListingReportRepository : ListingReportRepository {
    private val mutex = Mutex()
    private val reports = mutableListOf<StoredListingReport>()

    override suspend fun submit(report: ListingReport): Result<Unit> = mutex.withLock {
        if (reports.any {
                it.isOpen && it.report.listingId == report.listingId &&
                    it.report.reporterId == report.reporterId
            }
        ) {
            return@withLock Result.Error(ListingReportAlreadySubmittedException())
        }
        reports += StoredListingReport(report)
        Result.Success(Unit)
    }

    override suspend fun hasOpenReport(
        reporterId: MarketplaceReporterId,
        listingId: MarketplaceItemId
    ): Result<Boolean> = mutex.withLock {
        Result.Success(
            reports.any {
                it.isOpen && it.report.reporterId == reporterId && it.report.listingId == listingId
            }
        )
    }

    override suspend fun reportedListingIds(
        reporterId: MarketplaceReporterId
    ): Result<Set<MarketplaceItemId>> = mutex.withLock {
        Result.Success(
            reports.asSequence()
                .filter {
                    it.isOpen && it.report.reporterId == reporterId
                }
                .map { it.report.listingId }
                .toSet()
        )
    }

    internal suspend fun markClosedForTesting(report: ListingReport) = mutex.withLock {
        reports.firstOrNull { it.report == report && it.isOpen }?.isOpen = false
    }

    fun submittedReports(): List<ListingReport> = reports.map(StoredListingReport::report)

    private data class StoredListingReport(val report: ListingReport, var isOpen: Boolean = true)
}
