package com.jdrms.bulletin.domain.profile.domain.model

data class Profile(
    val id: UserId,
    val email: Email,
    val fullName: String,
    val university: String,
    val bio: String = "",
    val isVerified: Boolean = false
)

data class Session(
    val userId: UserId,
    val token: String,
    val isLoggedIn: Boolean = true
)
