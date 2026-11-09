package com.jdrms.bulletin.domain.profile.infrastructure.mapper

import com.jdrms.bulletin.domain.profile.domain.model.PublicProfileIdentity
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.infrastructure.dto.PublicProfileIdentityDto

object PublicProfileIdentityMapper {
    fun toDomain(dto: PublicProfileIdentityDto): PublicProfileIdentity = PublicProfileIdentity(
        userId = UserId(dto.id),
        fullName = dto.fullName?.trim()?.takeIf(String::isNotBlank),
        avatarUrl = dto.avatarUrl?.trim()?.takeIf(String::isNotBlank)
    )
}
