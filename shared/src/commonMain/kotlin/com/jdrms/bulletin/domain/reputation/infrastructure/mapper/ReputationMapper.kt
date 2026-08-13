package com.jdrms.bulletin.domain.reputation.infrastructure.mapper

import com.jdrms.bulletin.domain.reputation.domain.model.*
import com.jdrms.bulletin.domain.reputation.infrastructure.dto.ReviewDto

object ReputationMapper {
    fun toDomain(dto: ReviewDto): Review {
        return Review(
            id = ReviewId(dto.id),
            reviewerId = ReviewerId(dto.reviewerId),
            reviewerName = dto.reviewerName,
            revieweeId = RevieweeId(dto.revieweeId),
            rating = Rating(dto.score.coerceIn(1, 5)),
            comment = dto.comment,
            createdAtMillis = dto.createdAtMillis
        )
    }

    fun toDto(domain: Review): ReviewDto {
        return ReviewDto(
            id = domain.id.value,
            reviewerId = domain.reviewerId.value,
            reviewerName = domain.reviewerName,
            revieweeId = domain.revieweeId.value,
            score = domain.rating.score,
            comment = domain.comment,
            createdAtMillis = domain.createdAtMillis
        )
    }
}
