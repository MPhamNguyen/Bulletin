package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.profile.application.BlockUser
import com.jdrms.bulletin.domain.profile.application.UnblockUser
import com.jdrms.bulletin.domain.profile.domain.model.UserBlock
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.repository.UserBlockRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryUserBlockRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ProfileConversationUserBlockerTest {
    private val viewer = UserId("viewer")
    private val target = SenderId("other")
    private val auth = object : AuthRepository by InMemoryAuthRepository() {
        override suspend fun getCurrentUserId() = Result.Success(viewer)
    }

    @Test
    fun usesProfilePersistenceAndSignalsSuccessfulMutations() = runTest {
        val repository = InMemoryUserBlockRepository()
        val signal = RefreshSignal()
        var signals = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { signal.events.collect { signals++ } }
        val adapter = adapter(repository, signal)
        assertEquals(false, assertIs<Result.Success<Boolean>>(adapter.isBlocked(target)).data)
        assertIs<Result.Success<Unit>>(adapter.block(target))
        assertEquals(setOf(UserBlock(viewer, UserId(target.value))), repository.blocks())
        assertEquals(true, assertIs<Result.Success<Boolean>>(adapter.isBlocked(target)).data)
        assertIs<Result.Success<Unit>>(adapter.unblock(target))
        assertTrue(repository.blocks().isEmpty())
        assertEquals(2, signals)
        assertIs<Result.Error>(adapter.block(SenderId(viewer.value)))
        assertEquals(2, signals)
    }

    @Test
    fun readAndMutationErrorsPropagateWithoutSignalling() = runTest {
        val failure = Result.Error(IllegalStateException(), "Offline")
        val repository = object : UserBlockRepository by InMemoryUserBlockRepository() {
            override suspend fun getBlockedUserIds(viewerId: UserId): Result<Set<UserId>> = failure
            override suspend fun block(block: UserBlock): Result<Unit> = failure
        }
        val signal = RefreshSignal()
        var signals = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { signal.events.collect { signals++ } }
        val adapter = adapter(repository, signal)
        assertSame(failure, adapter.isBlocked(target))
        assertSame(failure, adapter.block(target))
        assertEquals(0, signals)
    }

    @Test
    fun signedOutStatusDoesNotUseFallbackIdentity() = runTest {
        val repository = InMemoryUserBlockRepository()
        val signedOut = object : AuthRepository by auth {
            override suspend fun getCurrentUserId() = Result.Success(null)
        }
        val adapter = ProfileConversationUserBlocker(
            signedOut,
            repository,
            BlockUser(signedOut, repository),
            UnblockUser(signedOut, repository),
            RefreshSignal()
        )
        assertIs<Result.Error>(adapter.isBlocked(target))
        assertIs<Result.Error>(adapter.block(target))
    }

    private fun adapter(repository: UserBlockRepository, signal: RefreshSignal) =
        ProfileConversationUserBlocker(
            auth,
            repository,
            BlockUser(auth, repository),
            UnblockUser(auth, repository),
            signal
        )
}
