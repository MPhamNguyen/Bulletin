package com.jdrms.bulletin.domain.create_listings.presentation

import com.jdrms.bulletin.domain.create_listings.domain.model.UserReputation

data class ReputationUiState(
    val userReputation: UserReputation? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showReviewDialog: Boolean = false,
    val newScore: Int = 5,
    val newComment: String = ""
)
