package com.jdrms.bulletin.domain.identity.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.identity.domain.model.Profile
import com.jdrms.bulletin.domain.identity.domain.model.Session
import com.jdrms.bulletin.domain.identity.domain.model.UserId

interface AuthRepository {
    suspend fun getCurrentSession(): Session?
    suspend fun login(email: String, password: String): Result<Session>
    suspend fun verifyEmail(email: String, code: String): Result<Boolean>
    suspend fun logout(): Result<Unit>
}

interface ProfileRepository {
    suspend fun getProfile(id: UserId): Profile?
    suspend fun updateProfile(profile: Profile): Result<Profile>
    suspend fun searchProfiles(query: String): List<Profile>
}
