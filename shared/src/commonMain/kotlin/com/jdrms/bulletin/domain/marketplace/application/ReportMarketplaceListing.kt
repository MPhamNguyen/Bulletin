package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingReport
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingReportReason
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId
import com.jdrms.bulletin.domain.marketplace.domain.repository.ListingReportRepository

fun interface MarketplaceReportIdentity {
    suspend fun currentReporterId(): Result<MarketplaceReporterId?>
}

class ReportMarketplaceListing(
    private val identity: MarketplaceReportIdentity,
    private val repository: ListingReportRepository
) {
    suspend operator fun invoke(
        listingId: MarketplaceItemId,
        listingSellerId: String,
        reason: ListingReportReason,
        description: String
    ): Result<Unit> {
        val reporterId = when (val result = identity.currentReporterId()) {
            is Result.Success -> result.data
            is Result.Error -> return result
        } ?: return Result.Error(IllegalStateException("Sign in to report a listing."))

        val reportCreation = ListingReport.create(
            listingId,
            listingSellerId,
            reporterId,
            reason,
            description
        )
        val report = when (reportCreation) {
            is Result.Success -> reportCreation.data
            is Result.Error -> return reportCreation
        }
        return repository.submit(report)
    }
}

class GetMarketplaceReportedListings(
    private val identity: MarketplaceReportIdentity,
    private val repository: ListingReportRepository
) : MarketplaceReportedListingProvider {
    override suspend fun getReportedListingIds(reporterId: MarketplaceReporterId): Set<MarketplaceItemId> {
        val authenticatedReporter = when (val result = identity.currentReporterId()) {
            is Result.Success -> result.data
            is Result.Error -> throw IllegalStateException(REPORTED_LISTINGS_FAILURE_MESSAGE, result.exception)
        } ?: return emptySet()
        if (authenticatedReporter != reporterId) return emptySet()
        return when (val result = repository.reportedListingIds(authenticatedReporter)) {
            is Result.Success -> result.data
            is Result.Error -> throw IllegalStateException(REPORTED_LISTINGS_FAILURE_MESSAGE, result.exception)
        }
    }

    private companion object {
        const val REPORTED_LISTINGS_FAILURE_MESSAGE = "Unable to load your reported listings."
    }
}
