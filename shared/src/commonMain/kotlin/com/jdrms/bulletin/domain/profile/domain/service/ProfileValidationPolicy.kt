package com.jdrms.bulletin.domain.profile.domain.service

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentReputation
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId

class ProfileValidationPolicy {
    fun validateUniversityRegistration(emailStr: String): Result<Unit> {
        val studentEmail = runCatching { StudentEmail(emailStr) }.getOrNull()
            ?: return Result.Error(IllegalArgumentException("Invalid email address format."))

        if (!studentEmail.isUniversityEmail) {
            return Result.Error(IllegalArgumentException("Bulletin requires a valid .edu university email."))
        }
        return Result.Success(Unit)
    }

    fun validateNewReview(review: StudentReview): Result<Unit> {
        if (review.reviewerId == review.revieweeId.value) {
            return Result.Error(IllegalArgumentException("Users cannot review themselves."))
        }
        if (review.comment.isBlank()) {
            return Result.Error(IllegalArgumentException("Review comment cannot be empty."))
        }
        return Result.Success(Unit)
    }

    fun calculateReputation(userId: UserId, reviews: List<StudentReview>): StudentReputation {
        if (reviews.isEmpty()) {
            return StudentReputation(userId = userId, averageRating = 5.0, totalReviews = 0, reviews = emptyList())
        }
        val avg = reviews.map { it.rating.score }.average()
        val roundedAvg = (avg * 10).toInt() / 10.0
        return StudentReputation(
            userId = userId,
            averageRating = roundedAvg,
            totalReviews = reviews.size,
            reviews = reviews
        )
    }
}
