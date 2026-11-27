package com.jdrms.bulletin.domain.messages.domain.model

enum class ConversationLockReason(val description: String) {
    ACCOUNT_DELETED(
        "This account no longer exists. You can view past messages, but you can't send or edit messages."
    ),
    LISTING_SOLD(
        "This listing has been sold. This conversation is read-only; you can't send or edit messages."
    ),
    LISTING_UNAVAILABLE(
        "This conversation isn't linked to a listing. You can view past messages, but you can't send or edit messages."
    )
}
