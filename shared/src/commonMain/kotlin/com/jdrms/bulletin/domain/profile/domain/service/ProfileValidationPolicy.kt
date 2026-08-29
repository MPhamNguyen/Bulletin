package com.jdrms.bulletin.domain.profile.domain.service

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentReputation
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId

class ProfileValidationPolicy {
    fun validateRegistration(
        firstName: String,
        lastName: String,
        emailStr: String,
        password: String
    ): Result<Unit> {
        val trimmedFirst = firstName.trim()
        val trimmedLast = lastName.trim()
        val trimmedEmail = emailStr.trim()
        val errorMessage = when {
            trimmedFirst.isEmpty() -> "First name is required."
            trimmedLast.isEmpty() -> "Last name is required."
            trimmedEmail.isEmpty() -> "Email is required."
            !StudentEmail.isValid(trimmedEmail) -> "Invalid email address format."
            password.isEmpty() -> "Password is required."
            password.length < MIN_PASSWORD_LENGTH ->
                "Password must be at least $MIN_PASSWORD_LENGTH characters."
            else -> null
        }
        return if (errorMessage != null) {
            Result.Error(IllegalArgumentException(errorMessage))
        } else {
            Result.Success(Unit)
        }
    }

    fun validateRegistration(
        emailStr: String,
        password: String,
        fullName: String
    ): Result<Unit> {
        val trimmedName = fullName.trim()
        val trimmedEmail = emailStr.trim()
        val errorMessage = when {
            trimmedName.isEmpty() -> "Full name is required."
            trimmedEmail.isEmpty() -> "Email is required."
            !StudentEmail.isValid(trimmedEmail) -> "Invalid email address format."
            password.isEmpty() -> "Password is required."
            password.length < MIN_PASSWORD_LENGTH ->
                "Password must be at least $MIN_PASSWORD_LENGTH characters."
            else -> null
        }
        return if (errorMessage != null) {
            Result.Error(IllegalArgumentException(errorMessage))
        } else {
            Result.Success(Unit)
        }
    }

    fun validateLogin(
        emailStr: String,
        password: String
    ): Result<Unit> {
        val trimmedEmail = emailStr.trim()
        val errorMessage = when {
            trimmedEmail.isEmpty() -> "Email is required."
            !StudentEmail.isValid(trimmedEmail) -> "Invalid email address format."
            password.isEmpty() -> "Password is required."
            else -> null
        }
        return if (errorMessage != null) {
            Result.Error(IllegalArgumentException(errorMessage))
        } else {
            Result.Success(Unit)
        }
    }

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

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}
