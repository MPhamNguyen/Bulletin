package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.domain.profile.domain.model.Profile
import com.jdrms.bulletin.domain.profile.domain.model.Session

data class IdentityUiState(
    val currentSession: Session? = null,
    val profile: Profile? = null,
    val searchResults: List<Profile> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val emailInput: String = "student@csulb.edu",
    val passwordInput: String = "password123",
    val verificationCodeInput: String = "1234",
    val searchQuery: String = ""
)
