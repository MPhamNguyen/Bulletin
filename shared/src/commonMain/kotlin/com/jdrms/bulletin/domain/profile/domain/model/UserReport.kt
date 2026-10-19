package com.jdrms.bulletin.domain.profile.domain.model

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.reporting.domain.model.ReportReason
import com.jdrms.bulletin.domain.reporting.domain.model.ReportType

data class UserReport(
    val reporterId: UserId,
    val reportedUserId: UserId,
    val reason: ReportReason,
    val description: String
) {
    init {
        require(reason.appliesTo(ReportType.USER)) { "Report reason is not valid for users." }
        require(reporterId.value.isNotBlank() && reportedUserId.value.isNotBlank()) {
            "Reporter and reported user IDs are required."
        }
        require(reporterId != reportedUserId) { "You cannot report your own account." }
        require(description.length <= MAX_DESCRIPTION_LENGTH) {
            "Description must be $MAX_DESCRIPTION_LENGTH characters or fewer."
        }
        require(description == description.trim()) { "Report description must be trimmed." }
    }

    companion object {
        const val MAX_DESCRIPTION_LENGTH = 1_000

        fun create(
            reporterId: UserId,
            reportedUserId: UserId,
            reason: ReportReason,
            description: String
        ): Result<UserReport> {
            if (!reason.appliesTo(ReportType.USER)) {
                return Result.Error(IllegalArgumentException("Report reason is not valid for users."))
            }
            val normalizedDescription = description.trim()
            if (reporterId.value.isBlank() || reportedUserId.value.isBlank()) {
                return Result.Error(IllegalArgumentException("Reporter and reported user IDs are required."))
            }
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
