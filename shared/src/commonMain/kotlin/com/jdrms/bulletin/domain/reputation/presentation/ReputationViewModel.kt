package com.jdrms.bulletin.domain.reputation.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.generateUuid
import com.jdrms.bulletin.domain.reputation.application.GetStudentReputation
import com.jdrms.bulletin.domain.reputation.application.SubmitReview
import com.jdrms.bulletin.domain.reputation.domain.model.Rating
import com.jdrms.bulletin.domain.reputation.domain.model.Review
import com.jdrms.bulletin.domain.reputation.domain.model.ReviewId
import com.jdrms.bulletin.domain.reputation.domain.model.RevieweeId
import com.jdrms.bulletin.domain.reputation.domain.model.ReviewerId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReputationViewModel(
    private val getStudentReputation: GetStudentReputation,
    private val submitReview: SubmitReview,
    // TODO: Drive reviewer & target user identity dynamically from authenticated session / navigation state
    private val targetUserId: RevieweeId = RevieweeId("user_101"),
    private val currentReviewerId: ReviewerId = ReviewerId("user_102"),
    private val currentReviewerName: String = "Campus User"
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReputationUiState())
    val uiState: StateFlow<ReputationUiState> = _uiState.asStateFlow()

    init {
        loadReputation()
    }

    fun loadReputation(userId: RevieweeId = targetUserId) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val rep = getStudentReputation(userId)
            _uiState.update { it.copy(userReputation = rep, isLoading = false) }
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

    fun submitNewReview() {
        val review = Review(
            id = ReviewId("rev_" + generateUuid()),
            reviewerId = currentReviewerId,
            reviewerName = currentReviewerName,
            revieweeId = targetUserId,
            rating = Rating(_uiState.value.newScore),
            comment = _uiState.value.newComment,
            createdAtMillis = currentTimeMillis()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val res = submitReview(review)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            showReviewDialog = false,
                            newComment = "",
                            errorMessage = null,
                            isLoading = false
                        )
                    }
                    loadReputation()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = res.message, isLoading = false) }
                }
            }
        }
    }
}
