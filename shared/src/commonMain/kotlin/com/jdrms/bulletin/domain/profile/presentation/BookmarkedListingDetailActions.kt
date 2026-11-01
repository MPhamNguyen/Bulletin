package com.jdrms.bulletin.domain.profile.presentation

internal data class BookmarkedListingReviewActions(
    val isAvailable: Boolean = false,
    val onOpen: () -> Unit = {},
    val isOpen: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val onDismiss: () -> Unit = {},
    val onSubmit: (score: Int, comment: String) -> Unit = { _, _ -> }
)

internal data class BookmarkedListingDetailActions(
    val onDismiss: () -> Unit,
    val onRemoveBookmark: () -> Unit,
    val canMessageSeller: Boolean,
    val onMessageSeller: suspend (String) -> Boolean,
    val hasExistingConversation: suspend () -> Boolean,
    val onMessageSent: () -> Unit,
    val review: BookmarkedListingReviewActions = BookmarkedListingReviewActions(),
    val onSeeChat: suspend (listingId: String, sellerId: String) -> Unit
)
