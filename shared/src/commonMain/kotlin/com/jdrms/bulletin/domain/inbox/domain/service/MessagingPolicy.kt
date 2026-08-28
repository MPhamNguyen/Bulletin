package com.jdrms.bulletin.domain.inbox.domain.service

import com.jdrms.bulletin.core.common.Result

class MessagingPolicy {
    fun validateMessageContent(text: String): Result<Unit> {
        if (text.isBlank()) {
            return Result.Error(IllegalArgumentException("Message content cannot be blank."))
        }
        if (text.length > 2000) {
            return Result.Error(IllegalArgumentException("Message exceeds maximum length of 2000 characters."))
        }
        return Result.Success(Unit)
    }

    fun validateReportReason(reason: String): Result<Unit> {
        if (reason.isBlank()) {
            return Result.Error(IllegalArgumentException("Report reason must be provided."))
        }
        return Result.Success(Unit)
    }
}
