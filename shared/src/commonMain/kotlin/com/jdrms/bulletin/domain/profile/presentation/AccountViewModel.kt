package com.jdrms.bulletin.domain.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.UserMessenger
import com.jdrms.bulletin.domain.profile.application.SessionRepository
import com.jdrms.bulletin.domain.profile.application.SessionState
import com.jdrms.bulletin.domain.profile.application.SignOutUser
import com.jdrms.bulletin.domain.profile.application.SoftDeleteProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountUiState(val isLoading: Boolean = false, val errorMessage: String? = null)

class AccountViewModel(
    private val sessionRepository: SessionRepository,
    private val signOutUser: SignOutUser,
    private val softDeleteProfile: SoftDeleteProfile,
    private val messenger: UserMessenger
) : ViewModel() {
    private val state = MutableStateFlow(AccountUiState())
    val uiState = state.asStateFlow()

    fun signOut(onSuccess: () -> Unit = {}) {
        if (state.value.isLoading) return
        state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = signOutUser()) {
                is Result.Success -> {
                    sessionRepository.unauthenticated()
                    state.value = AccountUiState()
                    onSuccess()
                }
                is Result.Error -> reportFailure(result.exception.message ?: "Failed to sign out")
            }
        }
    }

    fun deleteProfile(onSuccess: () -> Unit = {}) {
        val profile = (sessionRepository.state.value as? SessionState.Authenticated)?.profile ?: return
        if (state.value.isLoading) return
        state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = softDeleteProfile(profile)) {
                is Result.Success -> {
                    sessionRepository.unauthenticated()
                    state.value = AccountUiState()
                    onSuccess()
                }
                is Result.Error -> reportFailure(result.exception.message ?: "Failed to delete profile")
            }
        }
    }

    private fun reportFailure(message: String) {
        state.update { it.copy(isLoading = false, errorMessage = message) }
        messenger.show(message)
    }
}
