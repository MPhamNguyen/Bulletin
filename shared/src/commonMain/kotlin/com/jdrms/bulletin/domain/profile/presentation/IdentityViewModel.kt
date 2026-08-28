package com.jdrms.bulletin.domain.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.AuthenticateUser
import com.jdrms.bulletin.domain.profile.application.ManageProfile
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class IdentityViewModel(
    private val authenticateUser: AuthenticateUser,
    private val verifyStudentEmail: VerifyStudentEmail,
    private val manageProfile: ManageProfile
) : ViewModel() {

    private val _uiState = MutableStateFlow(IdentityUiState())
    val uiState: StateFlow<IdentityUiState> = _uiState.asStateFlow()

    init {
        loadCurrentSession()
    }

    fun loadCurrentSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val session = authenticateUser.getCurrentSession()
            val profile = session?.let { manageProfile.getProfile(it.userId) }
            _uiState.update {
                it.copy(
                    currentSession = session,
                    profile = profile,
                    isLoading = false
                )
            }
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(emailInput = email) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(passwordInput = password) }
    }

    fun onVerificationCodeChanged(code: String) {
        _uiState.update { it.copy(verificationCodeInput = code) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            val results = manageProfile.searchProfiles(query)
            _uiState.update { it.copy(searchResults = results) }
        }
    }

    fun login() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authenticateUser.login(_uiState.value.emailInput, _uiState.value.passwordInput)) {
                is Result.Success -> {
                    val profile = manageProfile.getProfile(result.data.userId)
                    _uiState.update {
                        it.copy(currentSession = result.data, profile = profile, isLoading = false)
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message, isLoading = false) }
                }
            }
        }
    }

    fun verifyEmail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = verifyStudentEmail(_uiState.value.emailInput, _uiState.value.verificationCodeInput)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Email verified successfully!") }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message, isLoading = false) }
                }
            }
        }
    }

    fun updateBio(newBio: String) {
        val currentProfile = _uiState.value.profile ?: return
        viewModelScope.launch {
            val updated = currentProfile.copy(bio = newBio)
            when (val result = manageProfile.updateProfile(updated)) {
                is Result.Success -> {
                    _uiState.update { it.copy(profile = result.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authenticateUser.logout()
            _uiState.update { it.copy(currentSession = null, profile = null) }
        }
    }
}
