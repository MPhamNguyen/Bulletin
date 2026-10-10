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
import kotlin.time.Instant

object ProfileMapper {
    fun toDomain(dto: ProfileDto, reputation: StudentReputation? = null): StudentProfile {
        return StudentProfile(
            id = UserId(dto.id),
            email = StudentEmail(dto.email),
            fullName = dto.fullName?.trim()?.takeIf(String::isNotBlank) ?: "Student",
            major = dto.major?.trim().orEmpty(),
            university = dto.university?.trim().orEmpty(),
            bio = dto.bio?.trim().orEmpty(),
            avatarUrl = dto.avatarUrl?.trim()?.takeIf(String::isNotBlank),
            isVerified = dto.isVerified ?: false,
            reputation = reputation,
            deletedAt = dto.deletedAt?.trim()?.takeIf(String::isNotBlank)
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
            avatarUrl = domain.avatarUrl,
            isVerified = domain.isVerified,
            deletedAt = domain.deletedAt
        )
    }

    fun toUpdateDto(domain: StudentProfile): ProfileUpdateDto {
        return ProfileUpdateDto(
            fullName = domain.fullName.trim(),
            major = domain.major.trim(),
            university = domain.university.trim(),
            bio = domain.bio.trim(),
            avatarUrl = domain.avatarUrl?.trim()?.ifBlank { null }
        )
    }

    fun toDomain(dto: ReviewDto): StudentReview {
        val clampedScore = dto.score.coerceIn(1, 5)
        return StudentReview(
            id = ReviewId(dto.id),
            reviewerId = dto.reviewerId,
            reviewerName = dto.reviewerName ?: "Student",
            revieweeId = UserId(dto.revieweeId),
            rating = Rating(clampedScore),
            comment = dto.comment,
            createdAtMillis = dto.createdAt.toEpochMillisecondsOrZero()
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
            createdAt = Instant.fromEpochMilliseconds(domain.createdAtMillis).toString()
        )
    }

    private fun String.toEpochMillisecondsOrZero(): Long {
        return if (isBlank()) 0L else Instant.parse(this).toEpochMilliseconds()
    }
}
