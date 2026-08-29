package com.jdrms.bulletin.domain.messages.domain.service

import com.jdrms.bulletin.core.common.Result

class MessagesPolicy {
    fun validateMessageContent(content: String): Result<Unit> {
        if (content.isBlank()) {
            return Result.Error(IllegalArgumentException("Message content cannot be blank."))
        }
        if (content.length > 1000) {
            return Result.Error(IllegalArgumentException("Message cannot exceed 1000 characters."))
        }
        return Result.Success(Unit)
    }

    fun validateReportReason(reason: String): Result<Unit> {
        if (reason.isBlank()) {
            return Result.Error(IllegalArgumentException("Report reason cannot be empty."))
        }
        return Result.Success(Unit)
    }
}
