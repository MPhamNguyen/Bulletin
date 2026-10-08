package com.jdrms.bulletin.domain.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.RequestPasswordReset
import com.jdrms.bulletin.domain.profile.application.SignOutUser
import com.jdrms.bulletin.domain.profile.application.UpdatePassword
import com.jdrms.bulletin.domain.profile.application.VerifyPasswordResetCode
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PasswordRecoveryUiState(
    val stage: PasswordRecoveryStage = PasswordRecoveryStage.NONE,
    val email: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class PasswordRecoveryViewModel(
    private val requestPasswordResetUseCase: RequestPasswordReset,
    private val verifyPasswordResetCodeUseCase: VerifyPasswordResetCode,
    private val updatePasswordUseCase: UpdatePassword,
    private val signOutUser: SignOutUser
) : ViewModel() {
    private val _uiState = MutableStateFlow(PasswordRecoveryUiState())
    val uiState: StateFlow<PasswordRecoveryUiState> = _uiState.asStateFlow()

    fun beginPasswordReset() {
        if (_uiState.value.stage.hasRecoverySession()) invalidatePasswordRecoverySession()
        _uiState.update {
            it.copy(
                stage = PasswordRecoveryStage.ENTER_EMAIL,
                email = "",
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun cancelPasswordReset() {
        if (_uiState.value.stage.hasRecoverySession()) invalidatePasswordRecoverySession()
        _uiState.update { PasswordRecoveryUiState() }
    }

    fun requestPasswordReset(emailStr: String) {
        val trimmedEmail = emailStr.trim()
        if (!StudentEmail.isValid(trimmedEmail)) {
            _uiState.update { it.copy(errorMessage = "Enter a valid email address.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            when (val result = requestPasswordResetUseCase(StudentEmail(trimmedEmail))) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        stage = PasswordRecoveryStage.ENTER_CODE,
                        email = trimmedEmail,
                        isLoading = false,
                        infoMessage = "Enter the confirmation code to continue."
                    )
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.exception.message)
                }
            }
        }
    }

    fun verifyPasswordResetCode(code: String) {
        val email = _uiState.value.email
        if (email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Start a password reset before entering a code.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            when (val result = verifyPasswordResetCodeUseCase(StudentEmail(email), code)) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        stage = PasswordRecoveryStage.CHANGE_PASSWORD,
                        isLoading = false
                    )
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.exception.message)
                }
            }
        }
    }

    fun updatePassword(password: String, confirmation: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            when (val result = updatePasswordUseCase(password, confirmation)) {
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.exception.message)
                }
                is Result.Success -> when (val signOutResult = signOutUser()) {
                    is Result.Success -> {
                        _uiState.value = PasswordRecoveryUiState()
                        onSuccess()
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = signOutResult.exception.message ?: "Failed to finish password reset"
                        )
                    }
                }
            }
        }
    }

    private fun invalidatePasswordRecoverySession() {
        viewModelScope.launch { signOutUser() }
    }

    private fun PasswordRecoveryStage.hasRecoverySession(): Boolean =
        this == PasswordRecoveryStage.ENTER_CODE || this == PasswordRecoveryStage.CHANGE_PASSWORD
}
