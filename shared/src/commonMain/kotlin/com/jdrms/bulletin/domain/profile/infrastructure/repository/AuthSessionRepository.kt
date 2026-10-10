package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.SessionRepository
import com.jdrms.bulletin.domain.profile.application.SessionState
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthSessionRepository(private val authRepository: AuthRepository) : SessionRepository {
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Checking)
    override val state: StateFlow<SessionState> = mutableState.asStateFlow()

    override suspend fun restore(): Result<StudentProfile?> {
        val result = authRepository.getCurrentUser()
        when (result) {
            is Result.Success -> result.data?.let(::authenticated) ?: unauthenticated()
            is Result.Error -> unauthenticated()
        }
        return result
    }

    override fun authenticated(profile: StudentProfile) {
        mutableState.value = SessionState.Authenticated(profile)
    }

    override fun unauthenticated() {
        mutableState.value = SessionState.Unauthenticated
    }
}
