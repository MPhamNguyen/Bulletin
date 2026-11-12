package com.jdrms.bulletin.domain.profile.domain.model

data class PublicProfileIdentity(
    val userId: UserId,
    val fullName: String?,
    val avatarUrl: String?
)
