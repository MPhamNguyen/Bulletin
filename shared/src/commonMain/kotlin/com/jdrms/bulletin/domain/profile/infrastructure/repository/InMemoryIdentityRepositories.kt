package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.Profile
import com.jdrms.bulletin.domain.profile.domain.model.Session
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.dto.SessionDto
import com.jdrms.bulletin.domain.profile.infrastructure.dto.UserDto
import com.jdrms.bulletin.domain.profile.infrastructure.mapper.IdentityMapper

/**
 * In-memory [AuthRepository] with a seeded mock session, for development before the
 * Supabase-backed implementation is wired up.
 */
class InMemoryAuthRepository : AuthRepository {

    private var currentSession: SessionDto? = SessionDto(
        userId = "user_101",
        token = "sb_mock_token_abc123",
        isLoggedIn = true
    )

    override suspend fun getCurrentSession(): Session? {
        return currentSession?.let { IdentityMapper.sessionToDomain(it) }
    }

    override suspend fun login(email: String, password: String): Result<Session> {
        val sessionDto = SessionDto(
            userId = "user_" + email.hashCode(),
            token = "jwt_token_" + email.hashCode(),
            isLoggedIn = true
        )
        currentSession = sessionDto
        return Result.Success(IdentityMapper.sessionToDomain(sessionDto))
    }

    override suspend fun verifyEmail(email: String, code: String): Result<Boolean> {
        return Result.Success(code == "1234" || code.length >= 4)
    }

    override suspend fun logout(): Result<Unit> {
        currentSession = null
        return Result.Success(Unit)
    }
}

/**
 * In-memory [ProfileRepository] with seeded mock profiles, for development before the
 * Supabase-backed implementation is wired up.
 */
class InMemoryProfileRepository : ProfileRepository {

    private val userProfiles = mutableMapOf<String, UserDto>(
        "user_101" to UserDto(
            id = "user_101",
            email = "student@csulb.edu",
            fullName = "Dominic Alfonso",
            university = "CSU Long Beach",
            bio = "Computer Science Student | Tech & Books Seller",
            isVerified = true
        )
    )

    override suspend fun getProfile(id: UserId): Profile? {
        return userProfiles[id.value]?.let { IdentityMapper.toDomain(it) }
    }

    override suspend fun updateProfile(profile: Profile): Result<Profile> {
        val dto = IdentityMapper.toDto(profile)
        userProfiles[dto.id] = dto
        return Result.Success(profile)
    }

    override suspend fun searchProfiles(query: String): List<Profile> {
        return userProfiles.values
            .filter { it.fullName.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true) }
            .map { IdentityMapper.toDomain(it) }
    }
}
