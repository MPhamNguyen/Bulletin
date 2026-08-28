package com.jdrms.bulletin.domain.profile.infrastructure.mapper

import com.jdrms.bulletin.domain.profile.domain.model.Email
import com.jdrms.bulletin.domain.profile.domain.model.Profile
import com.jdrms.bulletin.domain.profile.domain.model.Session
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.infrastructure.dto.SessionDto
import com.jdrms.bulletin.domain.profile.infrastructure.dto.UserDto

object IdentityMapper {
    fun toDomain(dto: UserDto): Profile {
        return Profile(
            id = UserId(dto.id),
            email = Email(dto.email),
            fullName = dto.fullName,
            university = dto.university,
            bio = dto.bio,
            isVerified = dto.isVerified
        )
    }

    fun toDto(domain: Profile): UserDto {
        return UserDto(
            id = domain.id.value,
            email = domain.email.value,
            fullName = domain.fullName,
            university = domain.university,
            bio = domain.bio,
            isVerified = domain.isVerified
        )
    }

    fun sessionToDomain(dto: SessionDto): Session {
        return Session(
            userId = UserId(dto.userId),
            token = dto.token,
            isLoggedIn = dto.isLoggedIn
        )
    }
}
