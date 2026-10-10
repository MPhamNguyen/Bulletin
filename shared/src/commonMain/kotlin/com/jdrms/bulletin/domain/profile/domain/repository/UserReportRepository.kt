package com.jdrms.bulletin.domain.profile.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.UserReport

interface UserReportRepository {
    suspend fun submit(report: UserReport): Result<Unit>
}
