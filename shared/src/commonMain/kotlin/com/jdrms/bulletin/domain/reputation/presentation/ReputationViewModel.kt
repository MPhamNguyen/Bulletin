package com.jdrms.bulletin.domain.reputation.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.reputation.application.GetStudentReputation
import com.jdrms.bulletin.domain.reputation.application.SubmitReview
import com.jdrms.bulletin.domain.reputation.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReputationViewModel(
    private val getStudentReputation: GetStudentReputation,
    private val submitReview: SubmitReview,
    private val targetUserId: RevieweeId = RevieweeId("user_101"),
    private val currentReviewerId: ReviewerId = ReviewerId("user_102"),
    private val currentReviewerName: String = "Sean Gallagher"
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReputationUiState())
    val uiState: StateFlow<ReputationUiState> = _uiState.asStateFlow()

    init {
        loadReputation()
    }

    fun loadReputation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val rep = getStudentReputation(targetUserId)
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
            id = ReviewId("rev_" + System.currentTimeMillis()),
            reviewerId = currentReviewerId,
            reviewerName = currentReviewerName,
            revieweeId = targetUserId,
            rating = Rating(_uiState.value.newScore),
            comment = _uiState.value.newComment,
            createdAtMillis = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val res = submitReview(review)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            showReviewDialog = false,
                            newComment = "",
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
