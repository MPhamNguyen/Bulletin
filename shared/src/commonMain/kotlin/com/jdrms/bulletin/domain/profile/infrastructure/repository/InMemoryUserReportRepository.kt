package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.UserReport
import com.jdrms.bulletin.domain.profile.domain.model.UserReportAlreadySubmittedException
import com.jdrms.bulletin.domain.profile.domain.repository.UserReportRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryUserReportRepository : UserReportRepository {
    private val mutex = Mutex()
    private val submittedReports = mutableListOf<StoredUserReport>()

    override suspend fun submit(report: UserReport): Result<Unit> = mutex.withLock {
        val alreadySubmitted = submittedReports.any {
            it.isOpen && it.report.reporterId == report.reporterId &&
                it.report.reportedUserId == report.reportedUserId
        }
        if (alreadySubmitted) return@withLock Result.Error(UserReportAlreadySubmittedException())
        submittedReports += StoredUserReport(report)
        Result.Success(Unit)
    }

    internal suspend fun markClosedForTesting(report: UserReport) = mutex.withLock {
        submittedReports.firstOrNull { it.report == report && it.isOpen }?.isOpen = false
    }

    fun reports(): List<UserReport> = submittedReports.map(StoredUserReport::report)

    private data class StoredUserReport(val report: UserReport, var isOpen: Boolean = true)
}
