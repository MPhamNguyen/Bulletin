package com.jdrms.bulletin.domain.home.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.domain.model.SwipeDecision
import com.jdrms.bulletin.domain.home.domain.repository.SwipeDecisionRepository

class RecordSwipeDecision(private val repository: SwipeDecisionRepository) {
    suspend operator fun invoke(userId: String?, decision: SwipeDecision): Result<Unit> {
        if (userId.isNullOrBlank()) return Result.Error(RecordSwipeDecisionFailure.NotSignedIn())
        return when (val result = repository.record(userId, decision)) {
            is Result.Success -> result
            is Result.Error -> Result.Error(RecordSwipeDecisionFailure.Storage(result.exception))
        }
    }
}

sealed class RecordSwipeDecisionFailure(cause: Throwable? = null) : Exception(null, cause) {
    class NotSignedIn : RecordSwipeDecisionFailure()
    class Storage(cause: Throwable) : RecordSwipeDecisionFailure(cause)
}
