package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.StudentReputation

data class ProfileUiState(
    val profile: StudentProfile? = null,
    val reputation: StudentReputation? = null,
    val showReviewDialog: Boolean = false,
    val newScore: Int = 5,
    val newComment: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isAccountCreated: Boolean = false
)
