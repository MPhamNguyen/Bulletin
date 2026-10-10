package com.jdrms.bulletin.domain.profile.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import kotlinx.coroutines.flow.StateFlow

sealed interface SessionState {
    data object Checking : SessionState
    data object Unauthenticated : SessionState
    data class Authenticated(val profile: StudentProfile) : SessionState
}

/** App-scoped source of the authenticated profile for presentation flows. */
interface SessionRepository {
    val state: StateFlow<SessionState>

    suspend fun restore(): Result<StudentProfile?>

    fun authenticated(profile: StudentProfile)

    fun unauthenticated()
}
