package com.jdrms.bulletin.domain.messages.domain.service

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.reporting.domain.model.ReportReason
import com.jdrms.bulletin.domain.reporting.domain.model.ReportType

class MessagesPolicy {
    companion object {
        const val MAX_REPORT_DESCRIPTION_LENGTH = 1_000
    }

    fun validateMessageContent(content: String): Result<Unit> {
        if (content.isBlank()) {
            return Result.Error(IllegalArgumentException("Message content cannot be blank."))
        }
        if (content.length > 1000) {
            return Result.Error(IllegalArgumentException("Message cannot exceed 1000 characters."))
        }
        return Result.Success(Unit)
    }

    fun validateReport(reason: ReportReason, description: String): Result<Unit> {
        if (!reason.appliesTo(ReportType.MESSAGE)) {
            return Result.Error(IllegalArgumentException("Report reason is not valid for messages."))
        }
        if (description.trim().length > MAX_REPORT_DESCRIPTION_LENGTH) {
            return Result.Error(IllegalArgumentException("Description must be $MAX_REPORT_DESCRIPTION_LENGTH characters or fewer."))
        }
        return Result.Success(Unit)
    }
}
