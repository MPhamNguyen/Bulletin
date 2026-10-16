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
    private val reports = mutableListOf<ListingReport>()

    override suspend fun submit(report: ListingReport): Result<Unit> = mutex.withLock {
        if (reports.any { it.listingId == report.listingId && it.reporterId == report.reporterId }) {
            return@withLock Result.Error(ListingReportAlreadySubmittedException())
        }
        reports += report
        Result.Success(Unit)
    }

    override suspend fun reportedListingIds(
        reporterId: MarketplaceReporterId
    ): Result<Set<MarketplaceItemId>> = mutex.withLock {
        Result.Success(
            reports.asSequence().filter { it.reporterId == reporterId }
                .map(ListingReport::listingId)
                .toSet()
        )
    }

    fun submittedReports(): List<ListingReport> = reports.toList()
}
