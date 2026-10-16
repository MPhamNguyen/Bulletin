package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.messages.application.InboxIdentity
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.profile.application.SessionRepository
import com.jdrms.bulletin.domain.profile.application.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Converts the app session to the messages context's typed identity. */
class AuthInboxIdentity(private val sessionRepository: SessionRepository) : InboxIdentity {
    override val currentUserId: SenderId?
        get() = sessionRepository.state.value.toMessageSenderId()

    override fun observeUserIds(): Flow<SenderId?> = sessionRepository.state.map(SessionState::toMessageSenderId)
}

private fun SessionState.toMessageSenderId(): SenderId? =
    (this as? SessionState.Authenticated)?.profile?.id?.value?.let(::SenderId)
