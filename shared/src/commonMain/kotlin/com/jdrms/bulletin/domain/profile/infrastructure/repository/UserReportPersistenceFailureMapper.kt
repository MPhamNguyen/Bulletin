package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.domain.profile.domain.model.UserReportAlreadySubmittedException

internal object UserReportPersistenceFailureMapper {
    fun map(code: String?, cause: Exception): IllegalStateException {
        return if (code == UNIQUE_VIOLATION_CODE) {
            UserReportAlreadySubmittedException()
        } else {
            IllegalStateException("We couldn't submit your report right now. Please try again.", cause)
        }
    }

    private const val UNIQUE_VIOLATION_CODE = "23505"
}
