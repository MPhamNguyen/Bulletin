package com.jdrms.bulletin.domain.messages.domain.model

class ConversationReadOnlyException : IllegalStateException(
    "This conversation is read-only because the other account was deleted."
)
