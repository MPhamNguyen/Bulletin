package com.jdrms.bulletin.domain.messages.application

import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlinx.coroutines.flow.Flow

/** Session identity translated at the app composition boundary. */
interface InboxIdentity {
    val currentUserId: SenderId?
    fun observeUserIds(): Flow<SenderId?>
}
