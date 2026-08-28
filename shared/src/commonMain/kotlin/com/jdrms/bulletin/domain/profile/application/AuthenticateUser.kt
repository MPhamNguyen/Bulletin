package com.jdrms.bulletin.domain.profile.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.Session
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository

class AuthenticateUser(
    private val authRepository: AuthRepository
) {
    suspend fun login(email: String, password: String): Result<Session> {
        if (email.isBlank() || password.isBlank()) {
            return Result.Error(IllegalArgumentException("Email and password must not be empty."))
        }
        return authRepository.login(email, password)
    }

    suspend fun getCurrentSession(): Session? {
        return authRepository.getCurrentSession()
    }

    suspend fun logout(): Result<Unit> {
        return authRepository.logout()
    }
}
