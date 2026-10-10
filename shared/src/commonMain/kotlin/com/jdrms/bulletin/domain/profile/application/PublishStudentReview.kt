package com.jdrms.bulletin.domain.profile.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.generateUuid
import com.jdrms.bulletin.domain.profile.domain.model.Rating
import com.jdrms.bulletin.domain.profile.domain.model.ReviewId
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId

class PublishStudentReview(
    private val submitStudentReview: SubmitStudentReview,
    private val newId: () -> ReviewId = { ReviewId("rev_${generateUuid().take(8)}") },
    private val nowMillis: () -> Long = ::currentTimeMillis
) {
    suspend operator fun invoke(
        targetId: UserId,
        reviewerId: UserId,
        reviewerName: String,
        score: Int,
        comment: String
    ): Result<Unit> {
        if (comment.isBlank()) return Result.Error(IllegalArgumentException("Review comment cannot be empty"))
        if (score !in 1..5) return Result.Error(IllegalArgumentException("Rating must be between 1 and 5."))
        val review = StudentReview(
            newId(),
            reviewerId.value,
            reviewerName,
            targetId,
            Rating(score),
            comment.trim(),
            nowMillis()
        )
        return submitStudentReview(targetId, review)
    }
}
