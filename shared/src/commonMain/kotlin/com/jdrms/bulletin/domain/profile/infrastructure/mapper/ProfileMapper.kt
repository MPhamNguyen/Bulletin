package com.jdrms.bulletin.domain.profile.infrastructure.mapper

import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.StudentReputation
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ProfileDto
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ProfileUpdateDto

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
}
