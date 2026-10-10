package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.core.common.Result

enum class MarketplaceUserReportReason(val label: String) {
    SCAM_OR_FRAUD("Scam or Fraud"),
    SOLICITATION_OF_PERSONAL_INFORMATION("Solicitation of Personal Information"),
    INAPPROPRIATE_CONTENT_OR_BEHAVIOR("Inappropriate Content or Behavior"),
    FAKE_ACCOUNT_OR_IMPERSONATION("Fake Account / Impersonation"),
    VIOLATION_OF_COMMERCE_POLICIES("Violation of Commerce Policies")
}

fun interface MarketplaceUserReporter {
    suspend fun report(userId: String, reason: MarketplaceUserReportReason, description: String): Result<Unit>
}

class ReportMarketplaceUser(private val reporter: MarketplaceUserReporter) {
    suspend operator fun invoke(
        userId: String,
        reason: MarketplaceUserReportReason,
        description: String
    ): Result<Unit> = reporter.report(userId, reason, description)
}
