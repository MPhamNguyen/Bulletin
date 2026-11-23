package com.jdrms.bulletin.domain.messages.domain.model

class ConversationReadOnlyException(
    val reason: ConversationLockReason = ConversationLockReason.ACCOUNT_DELETED
) : IllegalStateException(reason.description)
