package com.jdrms.bulletin.core.designsystem

internal data class MessageSellerQuickActionStatus(
    val fieldText: String,
    val buttonText: String,
    val isEditable: Boolean,
    val isMessageSent: Boolean,
    val canSend: Boolean = true
)
