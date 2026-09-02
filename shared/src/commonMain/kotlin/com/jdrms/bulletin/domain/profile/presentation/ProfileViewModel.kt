package com.jdrms.bulletin.domain.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.generateUuid
import com.jdrms.bulletin.domain.profile.application.AuthenticateUser
import com.jdrms.bulletin.domain.profile.application.ManageProfile
import com.jdrms.bulletin.domain.profile.application.SubmitStudentReview
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.Rating
import com.jdrms.bulletin.domain.profile.domain.model.ReviewId
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authenticateUser: AuthenticateUser,
    private val verifyStudentEmail: VerifyStudentEmail,
    private val manageProfile: ManageProfile,
    private val submitStudentReview: SubmitStudentReview,
    private val policy: ProfileValidationPolicy = ProfileValidationPolicy(),
    private val defaultUserId: UserId = UserId("current_student")
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile(defaultUserId)
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
                        isAccountCreated = true,
                        successMessage = "Account created successfully!",
                        errorMessage = null
                    )
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

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun resetRegistration() {
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
                            errorMessage = null,
                            isAccountCreated = true
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
}
