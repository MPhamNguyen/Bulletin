package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.UserReport
import com.jdrms.bulletin.domain.profile.domain.model.UserReportAlreadySubmittedException
import com.jdrms.bulletin.domain.profile.domain.repository.UserReportRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryUserReportRepository : UserReportRepository {
    private val mutex = Mutex()
    private val submittedReports = mutableListOf<UserReport>()

    override suspend fun submit(report: UserReport): Result<Unit> = mutex.withLock {
        val alreadySubmitted = submittedReports.any {
            it.reporterId == report.reporterId && it.reportedUserId == report.reportedUserId
        }
        if (alreadySubmitted) return@withLock Result.Error(UserReportAlreadySubmittedException())
        submittedReports += report
        Result.Success(Unit)
    }

    fun reports(): List<UserReport> = submittedReports.toList()
}
