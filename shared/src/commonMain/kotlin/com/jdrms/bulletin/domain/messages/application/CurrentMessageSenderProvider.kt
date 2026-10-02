package com.jdrms.bulletin.domain.messages.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

data class MessageSender(val id: SenderId, val displayName: String)

/** Resolves the current session on every operation; implementations must not cache a previous account. */
fun interface CurrentMessageSenderProvider {
    suspend fun getCurrentSender(): Result<MessageSender>
}

class MessagingAuthenticationRequiredException : IllegalStateException("Sign in to access your messages.")

class MessageSenderLookupException(cause: Throwable) :
    IllegalStateException("Unable to verify your messaging identity. Please try again.", cause)
