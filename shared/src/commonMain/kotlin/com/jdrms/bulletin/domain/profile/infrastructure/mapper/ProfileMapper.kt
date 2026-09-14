package com.jdrms.bulletin.domain.profile.infrastructure.mapper

import com.jdrms.bulletin.domain.profile.domain.model.Rating
import com.jdrms.bulletin.domain.profile.domain.model.ReviewId
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.StudentReputation
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ProfileDto
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ProfileUpdateDto
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ReviewDto

object ProfileMapper {
    fun toDomain(dto: ProfileDto, reputation: StudentReputation? = null): StudentProfile {
        return StudentProfile(
            id = UserId(dto.id),
            email = StudentEmail(dto.email),
            fullName = dto.fullName,
            major = dto.major,
            university = dto.university,
            bio = dto.bio,
            isVerified = dto.isVerified,
            reputation = reputation
        )
    }

    fun toDto(domain: StudentProfile): ProfileDto {
        return ProfileDto(
            id = domain.id.value,
            email = domain.email.value,
            fullName = domain.fullName,
            major = domain.major,
            university = domain.university,
            bio = domain.bio,
            isVerified = domain.isVerified
        )
    }

    fun toUpdateDto(domain: StudentProfile): ProfileUpdateDto {
        return ProfileUpdateDto(
            fullName = domain.fullName,
            university = domain.university,
            bio = domain.bio
        )
    }

    fun toDomain(dto: ReviewDto): StudentReview {
        val clampedScore = dto.score.coerceIn(1, 5)
        return StudentReview(
            id = ReviewId(dto.id),
            reviewerId = dto.reviewerId,
            reviewerName = dto.reviewerName,
            revieweeId = UserId(dto.revieweeId),
            rating = Rating(clampedScore),
            comment = dto.comment,
            createdAtMillis = dto.createdAtMillis
        )
    }

    fun toDto(domain: StudentReview): ReviewDto {
        return ReviewDto(
            id = domain.id.value,
            reviewerId = domain.reviewerId,
            reviewerName = domain.reviewerName,
            revieweeId = domain.revieweeId.value,
            score = domain.rating.score,
            comment = domain.comment,
            createdAtMillis = domain.createdAtMillis
        )
    }
}
