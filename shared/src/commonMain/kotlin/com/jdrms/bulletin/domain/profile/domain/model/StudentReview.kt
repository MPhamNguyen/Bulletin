package com.jdrms.bulletin.domain.profile.domain.model

data class StudentReview(
    val id: ReviewId,
    val reviewerId: String?,
    val reviewerName: String,
    val revieweeId: UserId,
    val rating: Rating,
    val comment: String,
    val createdAtMillis: Long = 0L,
    val relatedListingId: String? = null,
    val wasListingReview: Boolean = relatedListingId != null
) {
    init {
        require(wasListingReview || comment.isNotBlank()) { "Review comment cannot be blank." }
    }
}
