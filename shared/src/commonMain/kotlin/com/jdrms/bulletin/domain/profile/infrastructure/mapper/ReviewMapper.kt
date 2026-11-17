package com.jdrms.bulletin.domain.profile.infrastructure.mapper

import com.jdrms.bulletin.domain.profile.domain.model.Rating
import com.jdrms.bulletin.domain.profile.domain.model.ReviewId
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ReviewDto
import kotlin.time.Instant

object ReviewMapper {
    fun toDomain(dto: ReviewDto): StudentReview = StudentReview(
        id = ReviewId(dto.id),
        reviewerId = dto.reviewerId,
        reviewerName = if (dto.reviewerId == null) {
            FALLBACK_REVIEWER_NAME
        } else {
            dto.reviewerName?.trim()?.takeIf(String::isNotBlank) ?: FALLBACK_REVIEWER_NAME
        },
        revieweeId = UserId(dto.revieweeId),
        rating = Rating(dto.score.coerceIn(1, 5)),
        comment = dto.comment,
        createdAtMillis = if (dto.createdAt.isBlank()) 0L else Instant.parse(dto.createdAt).toEpochMilliseconds(),
        relatedListingId = dto.listingId,
        wasListingReview = dto.wasListingReview || dto.listingId != null
    )

    fun toDto(domain: StudentReview): ReviewDto = ReviewDto(
        id = domain.id.value,
        reviewerId = domain.reviewerId,
        reviewerName = domain.reviewerName,
        revieweeId = domain.revieweeId.value,
        score = domain.rating.score,
        comment = domain.comment,
        createdAt = Instant.fromEpochMilliseconds(domain.createdAtMillis).toString(),
        listingId = domain.relatedListingId,
        wasListingReview = domain.wasListingReview
    )

    private const val FALLBACK_REVIEWER_NAME = "Student"
}
