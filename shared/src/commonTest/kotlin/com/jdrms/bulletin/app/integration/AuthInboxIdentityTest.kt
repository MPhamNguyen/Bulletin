package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.profile.application.SessionRepository
import com.jdrms.bulletin.domain.profile.application.SessionState
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AuthInboxIdentityTest {
    @Test
    fun sessionIdentityIsTranslatedAndClearedAcrossAccountChanges() = runTest {
        val session = FakeSession()
        val identity = AuthInboxIdentity(session)
        assertNull(identity.currentUserId)

        val observed = mutableListOf<SenderId?>()
        val job = backgroundScope.launch { identity.observeUserIds().take(3).toList(observed) }
        runCurrent()
        session.authenticated(profile("first"))
        runCurrent()
        assertEquals(SenderId("first"), identity.currentUserId)
        session.unauthenticated()
        runCurrent()
        assertNull(identity.currentUserId)
        assertEquals(listOf(null, SenderId("first"), null), observed)
        job.cancel()
    }
}

private fun profile(id: String) = StudentProfile(UserId(id), StudentEmail("student@example.edu"), "Student")

private class FakeSession : SessionRepository {
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Checking)
    override val state: StateFlow<SessionState> = mutableState
    override suspend fun restore(): Result<StudentProfile?> = Result.Success(null)
    override fun authenticated(profile: StudentProfile) { mutableState.value = SessionState.Authenticated(profile) }
    override fun unauthenticated() { mutableState.value = SessionState.Unauthenticated }
}
