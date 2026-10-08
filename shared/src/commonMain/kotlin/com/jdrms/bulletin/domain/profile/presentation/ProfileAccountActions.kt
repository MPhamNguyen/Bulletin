package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.generateUuid
import com.jdrms.bulletin.domain.profile.application.SignOutUser
import com.jdrms.bulletin.domain.profile.application.SoftDeleteProfile
import com.jdrms.bulletin.domain.profile.application.SubmitStudentReview
import com.jdrms.bulletin.domain.profile.domain.model.Rating
import com.jdrms.bulletin.domain.profile.domain.model.ReviewId
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ProfileAccountActions(
    private val uiState: MutableStateFlow<ProfileUiState>,
    private val scope: CoroutineScope,
    private val signOutUser: SignOutUser,
    private val softDeleteProfile: SoftDeleteProfile,
    private val submitStudentReview: SubmitStudentReview,
    private val loadProfile: (UserId) -> Unit,
    private val cancelFlashNotification: () -> Unit
) {
    fun signOut(onSuccess: () -> Unit) = scope.launch {
        uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        when (val result = signOutUser()) {
            is Result.Success -> {
                cancelFlashNotification()
                uiState.update {
                    ProfileUiState(
                        authSessionState = AuthSessionState.UNAUTHENTICATED
                    )
                }
                onSuccess()
            }
            is Result.Error -> uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = result.exception.message ?: "Failed to sign out"
                )
            }
        }
    }

    fun deleteProfile(onSuccess: () -> Unit) {
        val profile = uiState.value.profile ?: return
        scope.launch {
            uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = softDeleteProfile(profile)) {
                is Result.Success -> {
                    cancelFlashNotification()
                    uiState.update {
                        ProfileUiState(
                            authSessionState = AuthSessionState.UNAUTHENTICATED
                        )
                    }
                    onSuccess()
                }
                is Result.Error -> uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exception.message ?: "Failed to delete profile"
                    )
                }
            }
        }
    }

    fun showReviewModal(show: Boolean) = uiState.update { it.copy(showReviewDialog = show) }
    fun onScoreChanged(score: Int) = uiState.update { it.copy(newScore = score) }
    fun onCommentChanged(comment: String) = uiState.update { it.copy(newComment = comment) }

    fun submitReview(reviewerId: String, reviewerName: String) {
        val state = uiState.value
        val targetId = state.profile?.id ?: UserId("current_student")
        if (state.newComment.isBlank()) {
            return uiState.update {
                it.copy(
                    errorMessage = "Review comment cannot be empty"
                )
            }
        }
        val review = StudentReview(
            ReviewId("rev_${generateUuid().take(8)}"),
            reviewerId,
            reviewerName,
            targetId,
            Rating(state.newScore),
            state.newComment.trim(),
            currentTimeMillis()
        )
        scope.launch {
            when (submitStudentReview(targetId, review)) {
                is Result.Success -> {
                    uiState.update {
                        it.copy(
                            showReviewDialog = false,
                            newComment = "",
                            newScore = 5
                        )
                    }
                    loadProfile(targetId)
                }
                is Result.Error -> uiState.update { it.copy(errorMessage = "Failed to submit review") }
            }
        }
    }
}
