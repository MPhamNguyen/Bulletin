package com.jdrms.bulletin.domain.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.generateUuid
import com.jdrms.bulletin.domain.profile.application.AuthenticateUser
import com.jdrms.bulletin.domain.profile.application.ManageProfile
import com.jdrms.bulletin.domain.profile.application.ResendVerificationCode
import com.jdrms.bulletin.domain.profile.application.RestoreAuthenticatedProfile
import com.jdrms.bulletin.domain.profile.application.SignOutUser
import com.jdrms.bulletin.domain.profile.application.SubmitStudentReview
import com.jdrms.bulletin.domain.profile.application.UpdateStudentProfile
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationOutcome
import com.jdrms.bulletin.domain.profile.domain.model.PendingRegistration
import com.jdrms.bulletin.domain.profile.domain.model.Rating
import com.jdrms.bulletin.domain.profile.domain.model.ReviewId
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Manual DI keeps each focused authentication and profile use case explicit.
@Suppress("LongParameterList")
class ProfileViewModel(
    private val authenticateUser: AuthenticateUser,
    private val restoreAuthenticatedProfile: RestoreAuthenticatedProfile,
    private val signOutUser: SignOutUser,
    private val verifyStudentEmail: VerifyStudentEmail,
    private val resendVerificationCode: ResendVerificationCode,
    private val manageProfile: ManageProfile,
    private val updateStudentProfile: UpdateStudentProfile,
    private val submitStudentReview: SubmitStudentReview,
    private val policy: ProfileValidationPolicy = ProfileValidationPolicy(),
    private val defaultUserId: UserId = UserId("current_student")
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    private var flashNotificationJob: Job? = null

    init {
        restoreSession()
    }

    private fun restoreSession() {
        viewModelScope.launch {
            when (val result = restoreAuthenticatedProfile()) {
                is Result.Success -> {
                    val profile = result.data
                    val rep = profile?.let { manageProfile.getReputation(it.id) }
                    _uiState.update {
                        it.copy(
                            profile = profile,
                            reputation = rep ?: it.reputation,
                            profileDraft = profile?.let(ProfileDraft::from) ?: ProfileDraft(),
                            authSessionState = if (profile == null) {
                                AuthSessionState.UNAUTHENTICATED
                            } else {
                                AuthSessionState.AUTHENTICATED
                            }
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            authSessionState = AuthSessionState.UNAUTHENTICATED,
                            errorMessage = null
                        )
                    }
                }
            }
        }
    }

    fun loadProfile(userId: UserId = defaultUserId) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val profileResult = manageProfile.getProfile(userId)
            when (profileResult) {
                is Result.Success -> {
                    val studentProfile = profileResult.data
                    val rep = manageProfile.getReputation(userId)
                    _uiState.update {
                        it.copy(
                            profile = studentProfile,
                            profileDraft = studentProfile?.let(ProfileDraft::from) ?: ProfileDraft(),
                            reputation = rep,
                            isLoading = false
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = profileResult.exception.message ?: "Failed to load profile"
                        )
                    }
                }
            }
        }
    }

    fun createAccount(
        firstName: String,
        lastName: String,
        emailStr: String,
        passwordStr: String,
        university: String = "CSU Long Beach"
    ) {
        if (_uiState.value.isLoading) return
        val trimmedFirst = firstName.trim()
        val trimmedLast = lastName.trim()
        val trimmedEmail = emailStr.trim()

        val validationResult = policy.validateRegistration(
            firstName = trimmedFirst,
            lastName = trimmedLast,
            emailStr = trimmedEmail,
            password = passwordStr
        )
        if (validationResult is Result.Error) {
            _uiState.update { it.copy(errorMessage = validationResult.exception.message) }
            return
        }

        val studentEmail = StudentEmail(trimmedEmail)
        val fullName = "$trimmedFirst $trimmedLast"

        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            val result = authenticateUser.register(
                email = studentEmail,
                password = passwordStr,
                fullName = fullName,
                university = university
            )
            when (result) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        pendingRegistration = result.data,
                        profile = null,
                        isAccountCreated = false,
                        authSessionState = AuthSessionState.UNAUTHENTICATED
                    )
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.exception.message)
                }
            }
        }
    }

    private suspend fun handleVerificationResult(result: Result<EmailVerificationOutcome>) {
        when (result) {
            is Result.Success -> {
                when (val outcome = result.data) {
                    is EmailVerificationOutcome.ProfileAvailable -> completeVerifiedRegistration(outcome.profile)
                    EmailVerificationOutcome.ProfileRecoveryRequired -> _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            pendingRegistration = null,
                            verifiedEmailAwaitingProfile = state.pendingRegistration?.email,
                            errorMessage = null,
                            successMessage = "Your email is verified. Finish loading your profile to continue."
                        )
                    }
                }
            }
            is Result.Error -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exception.message ?: "Failed to create account"
                    )
                }
            }
        }
    }

    private suspend fun completeVerifiedRegistration(profile: StudentProfile) {
        val rep = manageProfile.getReputation(profile.id)
        _uiState.update {
            it.copy(
                isLoading = false,
                profile = profile,
                reputation = rep,
                profileDraft = ProfileDraft.from(profile),
                isAccountCreated = true,
                pendingRegistration = null,
                verifiedEmailAwaitingProfile = null,
                isEditingProfile = false,
                authSessionState = AuthSessionState.AUTHENTICATED,
                errorMessage = null
            )
        }
        showFlashNotification("Account created successfully!")
    }

    fun retryVerifiedProfile() {
        if (_uiState.value.isLoading || _uiState.value.verifiedEmailAwaitingProfile == null) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = restoreAuthenticatedProfile()) {
                is Result.Success -> result.data?.let { completeVerifiedRegistration(it) }
                    ?: _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Profile setup is not ready yet. Please try again.")
                    }
                is Result.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exception.message ?: "Unable to finish loading your profile."
                    )
                }
            }
        }
    }

    fun clearMessages() {
        flashNotificationJob?.cancel()
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun startEditingProfile() {
        val profile = _uiState.value.profile
        _uiState.update {
            it.copy(
                profileDraft = profile?.let(ProfileDraft::from) ?: ProfileDraft(),
                isEditingProfile = true,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun cancelEditingProfile() {
        val profile = _uiState.value.profile
        flashNotificationJob?.cancel()
        _uiState.update {
            it.copy(
                profileDraft = profile?.let(ProfileDraft::from) ?: ProfileDraft(),
                isEditingProfile = false,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun onProfileDraftChanged(profileDraft: ProfileDraft) {
        flashNotificationJob?.cancel()
        _uiState.update { it.copy(profileDraft = profileDraft, errorMessage = null, successMessage = null) }
    }

    fun resetProfileDraft() {
        val profile = _uiState.value.profile ?: return
        flashNotificationJob?.cancel()
        _uiState.update {
            it.copy(
                profileDraft = ProfileDraft.from(profile),
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun updateProfileDetails() {
        val state = _uiState.value
        val profile = state.profile
        if (profile == null) {
            _uiState.update { it.copy(errorMessage = "Profile is unavailable.") }
            return
        }
        val draft = state.profileDraft

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            when (
                val result = updateStudentProfile(
                    profile,
                    draft.fullName,
                    draft.major,
                    draft.university,
                    draft.bio
                )
            ) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            profile = result.data,
                            profileDraft = ProfileDraft.from(result.data),
                            isEditingProfile = false,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                    showFlashNotification("Profile updated")
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.exception.message ?: "Failed to update profile"
                        )
                    }
                }
            }
        }
    }

    fun resetRegistration() {
        if (_uiState.value.isLoading) return
        flashNotificationJob?.cancel()
        _uiState.update {
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

    fun login(emailStr: String, pass: String, onSuccess: () -> Unit = {}) {
        val trimmedEmail = emailStr.trim()
        val validationResult = policy.validateLogin(
            emailStr = trimmedEmail,
            password = pass
        )
        if (validationResult is Result.Error) {
            _uiState.update { it.copy(errorMessage = validationResult.exception.message) }
            return
        }

        viewModelScope.launch {
            val studentEmail = StudentEmail(trimmedEmail)
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authenticateUser.login(studentEmail, pass)
            when (result) {
                is Result.Success -> {
                    val rep = manageProfile.getReputation(result.data.id)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = result.data,
                            reputation = rep,
                            profileDraft = ProfileDraft.from(result.data),
                            errorMessage = null,
                            isAccountCreated = false,
                            isEditingProfile = false,
                            authSessionState = AuthSessionState.AUTHENTICATED
                        )
                    }
                    onSuccess()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.exception.message ?: "Failed to log in"
                        )
                    }
                }
            }
        }
    }

    fun signOut(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            when (val result = signOutUser()) {
                is Result.Success -> {
                    flashNotificationJob?.cancel()
                    _uiState.update {
                        ProfileUiState(authSessionState = AuthSessionState.UNAUTHENTICATED)
                    }
                    onSuccess()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.exception.message ?: "Failed to sign out"
                        )
                    }
                }
            }
        }
    }

    private fun showFlashNotification(message: String) {
        flashNotificationJob?.cancel()
        _uiState.update { it.copy(successMessage = message) }
        flashNotificationJob = viewModelScope.launch {
            delay(FLASH_NOTIFICATION_DURATION_MILLIS)
            _uiState.update { state ->
                if (state.successMessage == message) state.copy(successMessage = null) else state
            }
        }
    }

    fun verifyEmail(emailStr: String, code: String) {
        val state = _uiState.value
        if (state.isLoading) return
        val pending = state.pendingRegistration ?: return
        if (!StudentEmail.isValid(emailStr) || StudentEmail(emailStr) != pending.email) {
            _uiState.update { it.copy(errorMessage = "Use the email address you registered with.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            handleVerificationResult(verifyStudentEmail(pending.email, code))
        }
    }

    fun resendEmailCode(emailStr: String) {
        if (_uiState.value.isLoading || _uiState.value.verifiedEmailAwaitingProfile != null) return
        if (!StudentEmail.isValid(emailStr)) {
            _uiState.update { it.copy(errorMessage = "Enter the email address you registered with.") }
            return
        }
        val email = _uiState.value.pendingRegistration?.email ?: StudentEmail(emailStr)
        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            when (val result = resendVerificationCode(email)) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        pendingRegistration = PendingRegistration(email),
                        successMessage = "If verification is pending for this email, a new code has been sent."
                    )
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.exception.message)
                }
            }
        }
    }

    fun showReviewModal(show: Boolean) {
        _uiState.update { it.copy(showReviewDialog = show) }
    }

    fun onScoreChanged(score: Int) {
        _uiState.update { it.copy(newScore = score) }
    }

    fun onCommentChanged(comment: String) {
        _uiState.update { it.copy(newComment = comment) }
    }

    fun submitReview(reviewerId: String = "peer_reviewer", reviewerName: String = "Campus Peer") {
        val state = _uiState.value
        val targetId = state.profile?.id ?: defaultUserId

        if (state.newComment.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Review comment cannot be empty") }
            return
        }

        val review = StudentReview(
            id = ReviewId("rev_${generateUuid().take(8)}"),
            reviewerId = reviewerId,
            reviewerName = reviewerName,
            revieweeId = targetId,
            rating = Rating(state.newScore),
            comment = state.newComment.trim(),
            createdAtMillis = currentTimeMillis()
        )

        viewModelScope.launch {
            val result = submitStudentReview(targetId, review)
            if (result.isSuccess()) {
                _uiState.update { it.copy(showReviewDialog = false, newComment = "", newScore = 5) }
                loadProfile(targetId)
            } else {
                _uiState.update { it.copy(errorMessage = "Failed to submit review") }
            }
        }
    }

    companion object {
        const val FLASH_NOTIFICATION_DURATION_MILLIS = 3_000L
    }
}
