package com.jdrms.bulletin.domain.messages.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

/** Messages-owned boundary for the viewer's explicit blocks. */
interface ConversationUserBlocker {
    suspend fun isBlocked(userId: SenderId): Result<Boolean>
    suspend fun block(userId: SenderId): Result<Unit>
    suspend fun unblock(userId: SenderId): Result<Unit>
}
