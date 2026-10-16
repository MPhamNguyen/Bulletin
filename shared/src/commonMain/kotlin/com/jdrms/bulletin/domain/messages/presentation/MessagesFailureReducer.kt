package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.MessageSenderLookupException
import com.jdrms.bulletin.domain.messages.application.MessagingAuthenticationRequiredException
import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException

internal data class PresentedMessagesFailure(
    val state: MessagesUiState,
    val identityUnavailable: Boolean
)

internal fun MessagesUiState.withFailure(error: Result.Error): PresentedMessagesFailure {
    val identityUnavailable = error.exception is MessagingAuthenticationRequiredException ||
        error.exception is MessageSenderLookupException ||
        error.exception is ConversationAccessException
    val message = if (error.exception is MessagingAuthenticationRequiredException) {
        "Sign in to access your messages."
    } else {
        "Unable to complete the messaging request. Please try again."
    }
    val nextState = if (identityUnavailable) {
        MessagesUiState(errorMessage = message)
    } else {
        copy(
            isLoading = false,
            isLoadingMessages = false,
            isLoadingOlder = false,
            isSending = false,
            isReporting = false,
            errorMessage = message,
            statusMessage = null
        )
    }
    return PresentedMessagesFailure(nextState, identityUnavailable)
}
