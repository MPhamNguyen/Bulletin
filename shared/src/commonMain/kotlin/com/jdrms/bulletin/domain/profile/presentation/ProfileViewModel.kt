package com.jdrms.bulletin.domain.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.generateUuid
import com.jdrms.bulletin.domain.profile.application.AuthenticateUser
import com.jdrms.bulletin.domain.profile.application.ManageProfile
import com.jdrms.bulletin.domain.profile.application.RestoreAuthenticatedProfile
import com.jdrms.bulletin.domain.profile.application.SignOutUser
import com.jdrms.bulletin.domain.profile.application.SubmitStudentReview
import com.jdrms.bulletin.domain.profile.application.UpdateStudentProfile
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
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

class ProfileViewModel(
    private val authenticateUser: AuthenticateUser,
    private val restoreAuthenticatedProfile: RestoreAuthenticatedProfile,
    private val signOutUser: SignOutUser,
    private val verifyStudentEmail: VerifyStudentEmail,
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
                    _uiState.update {
                        it.copy(
                            profile = profile,
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
                            errorMessage = result.exception.message ?: "Failed to restore session"
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

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = authenticateUser.register(
                email = studentEmail,
                password = passwordStr,
                fullName = fullName,
                university = university
            )
            handleRegistrationResult(result)
        }
    }

    private fun handleRegistrationResult(result: Result<StudentProfile>) {
        when (result) {
            is Result.Success -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        profile = result.data,
                        profileDraft = ProfileDraft.from(result.data),
                        isAccountCreated = true,
                        errorMessage = null
                    )
                }
                showFlashNotification("Account created successfully!")
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

    fun clearMessages() {
        flashNotificationJob?.cancel()
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
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
        flashNotificationJob?.cancel()
        _uiState.update {
            it.copy(
                isAccountCreated = false,
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
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = result.data,
                            profileDraft = ProfileDraft.from(result.data),
                            errorMessage = null,
                            isAccountCreated = true,
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
        viewModelScope.launch {
            val studentEmail = runCatching { StudentEmail(emailStr) }.getOrNull()
            if (studentEmail == null) {
                _uiState.update { it.copy(errorMessage = "Invalid email format") }
                return@launch
            }
            val result = verifyStudentEmail(studentEmail, code)
            if (result.isError()) {
                _uiState.update { it.copy(errorMessage = "Email verification failed") }
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
