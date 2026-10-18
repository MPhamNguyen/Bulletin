package com.jdrms.bulletin.domain.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.SessionRepository
import com.jdrms.bulletin.domain.profile.application.SignInExternally
import com.jdrms.bulletin.domain.profile.application.SignInUser
import com.jdrms.bulletin.domain.profile.domain.model.ExternalSignInOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ExternalSignInStage { Idle, InProgress, Cancelled, Error }

data class SignInUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val externalStage: ExternalSignInStage = ExternalSignInStage.Idle
)

class SignInViewModel(
    private val signInUser: SignInUser,
    private val sessionRepository: SessionRepository,
    private val signInExternally: SignInExternally? = null
) : ViewModel() {
    private val state = MutableStateFlow(SignInUiState())
    val uiState = state.asStateFlow()

    fun signIn(email: String, password: String) {
        if (state.value.isLoading) return
        state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = signInUser(email, password)) {
                is Result.Success -> {
                    sessionRepository.authenticated(result.data)
                    state.update { it.copy(isLoading = false) }
                }
                is Result.Error -> state.update {
                    it.copy(isLoading = false, errorMessage = result.exception.message ?: "Failed to log in")
                }
            }
        }
    }

    fun signInWithMicrosoft() {
        val signIn = signInExternally ?: return
        if (state.value.isLoading) return
        state.update { it.copy(isLoading = true, errorMessage = null, externalStage = ExternalSignInStage.InProgress) }
        viewModelScope.launch {
            when (val outcome = signIn()) {
                is ExternalSignInOutcome.SignedIn -> {
                    sessionRepository.authenticated(outcome.profile)
                    state.update { it.copy(isLoading = false, externalStage = ExternalSignInStage.Idle) }
                }
                ExternalSignInOutcome.Cancelled -> state.update {
                    it.copy(isLoading = false, externalStage = ExternalSignInStage.Cancelled)
                }
                is ExternalSignInOutcome.Failed -> state.update {
                    it.copy(
                        isLoading = false,
                        externalStage = ExternalSignInStage.Error,
                        errorMessage = outcome.reason.userMessage()
                    )
                }
            }
        }
    }

    fun clearError() = state.update { it.copy(errorMessage = null) }
}
