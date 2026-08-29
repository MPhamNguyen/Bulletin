package com.jdrms.bulletin.domain.profile.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class UserId(val value: String)

@JvmInline
value class ReviewId(val value: String)

class StudentEmail private constructor(val value: String) {
    init {
        require(EMAIL_REGEX.matches(value)) {
            "Invalid student email format: $value"
        }
    }

    val isUniversityEmail: Boolean
        get() = value.lowercase().endsWith(".edu")

    override fun equals(other: Any?): Boolean = other is StudentEmail && value == other.value
    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = value

    companion object {
        val EMAIL_REGEX = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        operator fun invoke(email: String): StudentEmail = StudentEmail(email.trim())
        fun isValid(email: String): Boolean = EMAIL_REGEX.matches(email.trim())
    }
}

data class Rating(val score: Int) {
    init {
        require(score in 1..5) { "Rating score must be between 1 and 5 stars." }
    }
}

data class StudentReview(
    val id: ReviewId,
    val reviewerId: String,
    val reviewerName: String,
    val revieweeId: UserId,
    val rating: Rating,
    val comment: String,
    val createdAtMillis: Long = 0L
) {
    init {
        require(comment.isNotBlank()) { "Review comment cannot be blank." }
    }
}

data class StudentReputation(
    val userId: UserId,
    val averageRating: Double = 5.0,
    val totalReviews: Int = 0,
    val reviews: List<StudentReview> = emptyList()
)

data class StudentProfile(
    val id: UserId,
    val email: StudentEmail,
    val fullName: String,
    val university: String = "CSU Long Beach",
    val bio: String = "",
    val isVerified: Boolean = false,
    val reputation: StudentReputation? = null
)
