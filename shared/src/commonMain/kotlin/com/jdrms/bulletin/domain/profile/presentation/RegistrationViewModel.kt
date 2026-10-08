package com.jdrms.bulletin.domain.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.RegisterStudent
import com.jdrms.bulletin.domain.profile.application.ResendVerificationCode
import com.jdrms.bulletin.domain.profile.application.SessionRepository
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationOutcome
import com.jdrms.bulletin.domain.profile.domain.model.PendingRegistration
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface RegistrationStage {
    data object Form : RegistrationStage
    data class Verification(val pending: PendingRegistration) : RegistrationStage
    data class Recovery(val email: StudentEmail) : RegistrationStage
    data class Complete(val profile: StudentProfile) : RegistrationStage
}

data class RegistrationUiState(
    val stage: RegistrationStage = RegistrationStage.Form,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val informationMessage: String? = null
)

class RegistrationViewModel(
    private val registerStudent: RegisterStudent,
    private val verifyStudentEmail: VerifyStudentEmail,
    private val resendVerificationCode: ResendVerificationCode,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val state = MutableStateFlow(RegistrationUiState())
    val uiState = state.asStateFlow()

    fun register(firstName: String, lastName: String, email: String, password: String, university: String = "") {
        if (state.value.isLoading || state.value.stage != RegistrationStage.Form) return
        state.update { it.copy(isLoading = true, errorMessage = null, informationMessage = null) }
        viewModelScope.launch {
            when (val result = registerStudent(firstName, lastName, email, password, university)) {
                is Result.Success -> state.update {
                    it.copy(stage = RegistrationStage.Verification(result.data), isLoading = false)
                }
                is Result.Error -> state.update {
                    it.copy(isLoading = false, errorMessage = result.exception.message)
                }
            }
        }
    }

    fun verify(code: String) {
        val pending = (state.value.stage as? RegistrationStage.Verification)?.pending ?: return
        if (state.value.isLoading) return
        state.update { it.copy(isLoading = true, errorMessage = null, informationMessage = null) }
        viewModelScope.launch {
            when (val result = verifyStudentEmail(pending.email, code)) {
                is Result.Success -> when (val outcome = result.data) {
                    is EmailVerificationOutcome.ProfileAvailable -> {
                        sessionRepository.authenticated(outcome.profile)
                        state.update {
                            it.copy(stage = RegistrationStage.Complete(outcome.profile), isLoading = false)
                        }
                    }
                    EmailVerificationOutcome.ProfileRecoveryRequired -> state.update {
                        it.copy(
                            stage = RegistrationStage.Recovery(pending.email),
                            isLoading = false,
                            informationMessage = "Your email is verified. Finish loading your profile to continue."
                        )
                    }
                }
                is Result.Error -> state.update {
                    it.copy(isLoading = false, errorMessage = result.exception.message ?: "Failed to create account")
                }
            }
        }
    }

    fun retryProfile() {
        if (state.value.stage !is RegistrationStage.Recovery || state.value.isLoading) return
        state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = sessionRepository.restore()) {
                is Result.Success -> {
                    val profile = result.data
                    state.update {
                        if (profile == null) {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Profile setup is not ready yet. Please try again."
                            )
                        } else {
                            it.copy(stage = RegistrationStage.Complete(profile), isLoading = false)
                        }
                    }
                }
                is Result.Error -> state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exception.message ?: "Unable to finish loading your profile."
                    )
                }
            }
        }
    }

    fun resendCode() {
        val pending = (state.value.stage as? RegistrationStage.Verification)?.pending ?: return
        if (state.value.isLoading) return
        state.update { it.copy(isLoading = true, errorMessage = null, informationMessage = null) }
        viewModelScope.launch {
            when (val result = resendVerificationCode(pending.email)) {
                is Result.Success -> state.update {
                    it.copy(
                        isLoading = false,
                        informationMessage = "If verification is pending for this email, a new code has been sent."
                    )
                }
                is Result.Error -> state.update {
                    it.copy(isLoading = false, errorMessage = result.exception.message)
                }
            }
        }
    }

    fun reset() {
        if (!state.value.isLoading) state.value = RegistrationUiState()
    }

    fun clearError() = state.update { it.copy(errorMessage = null, informationMessage = null) }
}
