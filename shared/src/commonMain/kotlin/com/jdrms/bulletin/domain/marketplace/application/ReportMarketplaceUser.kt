package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.reporting.domain.model.ReportReason

fun interface MarketplaceUserReporter {
    suspend fun report(userId: String, reason: ReportReason, description: String): Result<Unit>
}

class ReportMarketplaceUser(private val reporter: MarketplaceUserReporter) {
    suspend operator fun invoke(
        userId: String,
        reason: ReportReason,
        description: String
    ): Result<Unit> = reporter.report(userId, reason, description)
}
