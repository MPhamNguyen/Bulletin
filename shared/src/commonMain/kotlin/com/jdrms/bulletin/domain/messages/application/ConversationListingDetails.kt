package com.jdrms.bulletin.domain.messages.application

/** Display snapshot retained from the listing response used to populate a conversation card. */
data class ConversationListingDetails(
    val sellerId: String,
    val sellerName: String,
    val category: String,
    val condition: String,
    val description: String,
    val photos: List<String>,
    val attributes: List<ConversationListingAttribute>,
    val createdAtMillis: Long,
    val updatedAtMillis: Long = 0L
)

data class ConversationListingAttribute(val label: String, val value: String)
