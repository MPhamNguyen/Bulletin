package com.jdrms.bulletin.domain.messages.infrastructure.repository

import com.jdrms.bulletin.domain.messages.domain.model.ConversationLockReason
import com.jdrms.bulletin.domain.messages.domain.model.ConversationReadOnlyException
import com.jdrms.bulletin.domain.messages.domain.model.MessageEditException
import com.jdrms.bulletin.domain.messages.domain.model.MessageEditFailure
import io.github.jan.supabase.postgrest.exception.PostgrestRestException

internal fun PostgrestRestException.toMessageWriteFailure(isEdit: Boolean): Exception {
    if (code == "BM006") return ConversationReadOnlyException(ConversationLockReason.LISTING_SOLD)
    val editFailure = if (isEdit) {
        when (code) {
            "BM001", "42501" -> MessageEditFailure.UNAVAILABLE
            "BM002" -> MessageEditFailure.EXPIRED
            "BM003" -> MessageEditFailure.REPORTED
            "BM004" -> MessageEditFailure.CONFLICT
            "BM005" -> MessageEditFailure.INVALID
            else -> null
        }
    } else {
        null
    }
    return editFailure?.let(::MessageEditException) ?: this
}
