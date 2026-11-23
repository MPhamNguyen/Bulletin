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
    /** Retains listing origin on reads after the listing is deleted. */
    val wasListingReview: Boolean = relatedListingId != null,
    val reviewerAvatarUrl: String? = null
)
