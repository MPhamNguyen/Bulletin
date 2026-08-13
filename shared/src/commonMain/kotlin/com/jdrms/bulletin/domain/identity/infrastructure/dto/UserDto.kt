package com.jdrms.bulletin.domain.identity.infrastructure.dto

data class UserDto(
    val id: String,
    val email: String,
    val fullName: String,
    val university: String,
    val bio: String = "",
    val isVerified: Boolean = false
)

data class SessionDto(
    val userId: String,
    val token: String,
    val isLoggedIn: Boolean
)
