package com.jdrms.bulletin.domain.marketplace.presentation

data class MarketplaceScreenActions(
    val onMessageSeller: suspend (String, String, String, String) -> Boolean = { _, _, _, _ -> false },
    val hasExistingConversation: suspend (String, String) -> Boolean = { _, _ -> false },
    val onSeeChat: suspend (String, String) -> Unit = { _, _ -> },
    val onListingClosed: () -> Unit = {},
    val onMessageSent: () -> Unit = {}
)
