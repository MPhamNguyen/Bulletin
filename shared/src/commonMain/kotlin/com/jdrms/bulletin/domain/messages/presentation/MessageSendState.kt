package com.jdrms.bulletin.domain.messages.presentation

/** A conversation's send status for the current messaging session. */
sealed interface MessageSendState {
    data object Sending : MessageSendState
    data class Failed(val message: String) : MessageSendState
}
