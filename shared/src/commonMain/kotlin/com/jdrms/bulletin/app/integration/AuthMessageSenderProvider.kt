package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.CurrentMessageSenderProvider
import com.jdrms.bulletin.domain.messages.application.MessageSender
import com.jdrms.bulletin.domain.messages.application.MessageSenderLookupException
import com.jdrms.bulletin.domain.messages.application.MessagesInstrumentation
import com.jdrms.bulletin.domain.messages.application.MessagingAuthenticationRequiredException
import com.jdrms.bulletin.domain.messages.application.NoOpMessagesInstrumentation
import com.jdrms.bulletin.domain.messages.application.measuredMessagesOperation
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.profile.application.RestoreAuthenticatedProfile

/** Composition boundary: only a messages-owned identity snapshot leaves this adapter. */
class AuthMessageSenderProvider(
    private val restoreAuthenticatedProfile: RestoreAuthenticatedProfile,
    private val instrumentation: MessagesInstrumentation = NoOpMessagesInstrumentation
) : CurrentMessageSenderProvider {
    override suspend fun getCurrentSender(): Result<MessageSender> {
        return measuredMessagesOperation(instrumentation, "sender_resolution") {
            when (val result = restoreAuthenticatedProfile()) {
                is Result.Success -> {
                    val profile = result.data
                    if (profile == null) {
                        Result.Error(MessagingAuthenticationRequiredException())
                    } else {
                        Result.Success(MessageSender(SenderId(profile.id.value), profile.fullName))
                    }
                }
                is Result.Error -> Result.Error(MessageSenderLookupException(result.exception))
            }
        }
    }
}
