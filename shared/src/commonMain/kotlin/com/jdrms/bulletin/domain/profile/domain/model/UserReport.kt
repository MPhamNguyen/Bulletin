package com.jdrms.bulletin.domain.profile.domain.model

import com.jdrms.bulletin.core.common.Result

enum class UserReportReason(val label: String) {
    SCAM_OR_FRAUD("Scam or Fraud"),
    SOLICITATION_OF_PERSONAL_INFORMATION("Solicitation of Personal Information"),
    INAPPROPRIATE_CONTENT_OR_BEHAVIOR("Inappropriate Content or Behavior"),
    FAKE_ACCOUNT_OR_IMPERSONATION("Fake Account / Impersonation"),
    VIOLATION_OF_COMMERCE_POLICIES("Violation of Commerce Policies")
}

data class UserReport(
    val reporterId: UserId,
    val reportedUserId: UserId,
    val reason: UserReportReason,
    val description: String
) {
    companion object {
        const val MAX_DESCRIPTION_LENGTH = 1_000

        fun create(
            reporterId: UserId,
            reportedUserId: UserId,
            reason: UserReportReason,
            description: String
        ): Result<UserReport> {
            val normalizedDescription = description.trim()
            if (reporterId == reportedUserId) {
                return Result.Error(IllegalArgumentException("You cannot report your own account."))
            }
            if (normalizedDescription.length > MAX_DESCRIPTION_LENGTH) {
                return Result.Error(
                    IllegalArgumentException("Description must be $MAX_DESCRIPTION_LENGTH characters or fewer.")
                )
            }
            return Result.Success(UserReport(reporterId, reportedUserId, reason, normalizedDescription))
        }
    }
}
