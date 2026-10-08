package com.jdrms.bulletin.domain.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.PublishStudentReview
import com.jdrms.bulletin.domain.profile.application.SessionRepository
import com.jdrms.bulletin.domain.profile.application.SessionState
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewUiState(
    val targetId: UserId? = null,
    val score: Int = 5,
    val comment: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

class ReviewViewModel(
    private val sessionRepository: SessionRepository,
    private val publishStudentReview: PublishStudentReview
) : ViewModel() {
    private val state = MutableStateFlow(ReviewUiState())
    val uiState = state.asStateFlow()

    fun begin(targetId: UserId) { state.value = ReviewUiState(targetId = targetId) }
    fun changeScore(score: Int) = state.update { it.copy(score = score) }
    fun changeComment(comment: String) = state.update { it.copy(comment = comment, errorMessage = null) }

    fun submit(onSubmitted: () -> Unit) {
        val reviewer = (sessionRepository.state.value as? SessionState.Authenticated)?.profile
        val target = state.value.targetId
        if (reviewer == null || target == null) {
            state.update { it.copy(errorMessage = "Review is unavailable.") }
            return
        }
        if (state.value.isSubmitting) return
        state.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            when (
                val result = publishStudentReview(
                    target,
                    reviewer.id,
                    reviewer.fullName,
                    state.value.score,
                    state.value.comment
                )
            ) {
                is Result.Success -> {
                    state.value = ReviewUiState()
                    onSubmitted()
                }
                is Result.Error -> state.update {
                    it.copy(isSubmitting = false, errorMessage = result.exception.message ?: "Failed to submit review")
                }
            }
        }
    }
}
