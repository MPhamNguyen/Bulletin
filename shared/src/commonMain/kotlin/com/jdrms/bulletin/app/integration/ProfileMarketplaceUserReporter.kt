package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceUserReporter
import com.jdrms.bulletin.domain.profile.application.SubmitUserReport
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.reporting.domain.model.ReportReason

class ProfileMarketplaceUserReporter(
    private val submitUserReport: SubmitUserReport
) : MarketplaceUserReporter {
    override suspend fun report(
        userId: String,
        reason: ReportReason,
        description: String
    ): Result<Unit> = submitUserReport(
        reportedUserId = UserId(userId),
        reason = reason,
        description = description
    )
}
