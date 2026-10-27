package com.jdrms.bulletin.domain.profile.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.model.UserReport
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.repository.UserReportRepository
import com.jdrms.bulletin.domain.reporting.domain.model.ReportReason

class SubmitUserReport(
    private val authRepository: AuthRepository,
    private val reportRepository: UserReportRepository
) {
    suspend operator fun invoke(
        reportedUserId: UserId,
        reason: ReportReason,
        description: String
    ): Result<Unit> {
        val reporterId = when (val result = authRepository.getCurrentUserId()) {
            is Result.Success -> result.data
            is Result.Error -> return result
        } ?: return Result.Error(IllegalStateException("Sign in to report a user."))

        val report = when (val result = UserReport.create(reporterId, reportedUserId, reason, description)) {
            is Result.Success -> result.data
            is Result.Error -> return result
        }
        return reportRepository.submit(report)
    }
}
