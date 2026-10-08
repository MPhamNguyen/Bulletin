package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.AuthenticateUser
import com.jdrms.bulletin.domain.profile.application.ManageProfile
import com.jdrms.bulletin.domain.profile.application.ProfileActiveListingsProvider
import com.jdrms.bulletin.domain.profile.application.ResendVerificationCode
import com.jdrms.bulletin.domain.profile.application.RestoreAuthenticatedProfile
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationOutcome
import com.jdrms.bulletin.domain.profile.domain.model.PendingRegistration
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileAuthenticationActions internal constructor(
    private val uiState: MutableStateFlow<ProfileUiState>,
    private val scope: CoroutineScope,
    private val authenticateUser: AuthenticateUser,
    private val restoreAuthenticatedProfile: RestoreAuthenticatedProfile,
    private val verifyStudentEmail: VerifyStudentEmail,
    private val resendVerificationCode: ResendVerificationCode,
    private val manageProfile: ManageProfile,
    private val activeListingsProvider: ProfileActiveListingsProvider?,
    private val policy: ProfileValidationPolicy,
    private val showFlashNotification: (String) -> Unit
) {
    fun restoreSession() = scope.launch {
        when (val result = restoreAuthenticatedProfile()) {
            is Result.Success -> setAuthenticatedProfile(result.data)
            is Result.Error -> uiState.update {
                it.copy(
                    authSessionState = AuthSessionState.UNAUTHENTICATED,
                    activeListingsCount = 0,
                    errorMessage = null
                )
            }
        }
    }

    fun createAccount(firstName: String, lastName: String, email: String, password: String, university: String) {
        if (uiState.value.isLoading) return
        val first = firstName.trim()
        val last = lastName.trim()
        val normalizedEmail = email.trim()
        when (val validation = policy.validateRegistration(first, last, normalizedEmail, password)) {
            is Result.Error -> uiState.update { it.copy(errorMessage = validation.exception.message) }
            is Result.Success -> {
                uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
                scope.launch {
                    val result = authenticateUser.register(
                        StudentEmail(normalizedEmail),
                        password,
                        "$first $last",
                        university
                    )
                    when (result) {
                        is Result.Success -> uiState.update {
                            it.copy(
                                isLoading = false,
                                pendingRegistration = result.data,
                                profile = null,
                                isAccountCreated = false,
                                authSessionState = AuthSessionState.UNAUTHENTICATED
                            )
                        }
                        is Result.Error -> uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.exception.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        val normalizedEmail = email.trim()
        when (val validation = policy.validateLogin(normalizedEmail, password)) {
            is Result.Error -> uiState.update { it.copy(errorMessage = validation.exception.message) }
            is Result.Success -> scope.launch {
                uiState.update { it.copy(isLoading = true, errorMessage = null) }
                when (val result = authenticateUser.login(StudentEmail(normalizedEmail), password)) {
                    is Result.Success -> {
                        setAuthenticatedProfile(result.data)
                        uiState.update { it.copy(isAccountCreated = false, isEditingProfile = false) }
                        onSuccess()
                    }
                    is Result.Error -> uiState.update {
                        it.copy(isLoading = false, errorMessage = result.exception.message ?: "Failed to log in")
                    }
                }
            }
        }
    }

    fun verifyEmail(email: String, code: String) {
        val state = uiState.value
        val pending = state.pendingRegistration ?: return
        if (state.isLoading) return
        if (!StudentEmail.isValid(email) || StudentEmail(email) != pending.email) {
            uiState.update { it.copy(errorMessage = "Use the email address you registered with.") }
            return
        }
        uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        scope.launch {
            when (val result = verifyStudentEmail(pending.email, code)) {
                is Result.Success -> when (val outcome = result.data) {
                    is EmailVerificationOutcome.ProfileAvailable -> {
                        setAuthenticatedProfile(outcome.profile)
                        uiState.update { it.copy(isAccountCreated = true, pendingRegistration = null) }
                        showFlashNotification("Account created successfully!")
                    }
                    EmailVerificationOutcome.ProfileRecoveryRequired -> uiState.update {
                        it.copy(
                            isLoading = false,
                            pendingRegistration = null,
                            verifiedEmailAwaitingProfile = pending.email,
                            errorMessage = null,
                            successMessage = "Your email is verified. Finish loading your profile to continue."
                        )
                    }
                }
                is Result.Error -> uiState.update {
                    it.copy(isLoading = false, errorMessage = result.exception.message ?: "Failed to create account")
                }
            }
        }
    }

    fun retryVerifiedProfile() {
        if (uiState.value.isLoading || uiState.value.verifiedEmailAwaitingProfile == null) return
        uiState.update { it.copy(isLoading = true, errorMessage = null) }
        scope.launch {
            when (val result = restoreAuthenticatedProfile()) {
                is Result.Success -> {
                    val profile = result.data
                    if (profile == null) {
                        uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Profile setup is not ready yet. Please try again."
                            )
                        }
                    } else {
                        setAuthenticatedProfile(profile)
                        uiState.update {
                            it.copy(
                                isAccountCreated = true,
                                pendingRegistration = null,
                                verifiedEmailAwaitingProfile = null
                            )
                        }
                        showFlashNotification("Account created successfully!")
                    }
                }
                is Result.Error -> uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exception.message ?: "Unable to finish loading your profile."
                    )
                }
            }
        }
    }

    fun resendEmailCode(email: String) {
        if (uiState.value.isLoading || uiState.value.verifiedEmailAwaitingProfile != null) return
        val normalizedEmail = email.trim()
        if (!StudentEmail.isValid(normalizedEmail)) {
            uiState.update { it.copy(errorMessage = "Enter the email address you registered with.") }
            return
        }
        val address = uiState.value.pendingRegistration?.email ?: StudentEmail(normalizedEmail)
        uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        scope.launch {
            when (val result = resendVerificationCode(address)) {
                is Result.Success -> uiState.update {
                    it.copy(
                        isLoading = false,
                        pendingRegistration = PendingRegistration(address),
                        successMessage = "If verification is pending for this email, a new code has been sent."
                    )
                }
                is Result.Error -> uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exception.message
                    )
                }
            }
        }
    }

    fun resetRegistration(cancelFlashNotification: () -> Unit) {
        if (uiState.value.isLoading) return
        cancelFlashNotification()
        uiState.update {
            it.copy(
                isAccountCreated = false,
                pendingRegistration = null,
                verifiedEmailAwaitingProfile = null,
                isEditingProfile = false,
                successMessage = null,
                errorMessage = null
            )
        }
    }

    private suspend fun setAuthenticatedProfile(profile: StudentProfile?) {
        val reputation = profile?.let { manageProfile.getReputation(it.id) }
        val activeCount = profile?.let { activeListingsProvider?.getActiveListingsCount(it.id) } ?: 0
        uiState.update {
            it.copy(
                profile = profile,
                reputation = reputation ?: it.reputation,
                activeListingsCount = activeCount,
                profileDraft = profile?.let(ProfileDraft::from) ?: ProfileDraft(),
                isLoading = false,
                activeSubscreen = ProfileSubscreen.PROFILE,
                authSessionState = if (profile == null) {
                    AuthSessionState.UNAUTHENTICATED
                } else {
                    AuthSessionState.AUTHENTICATED
                }
            )
        }
    }
}
