package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.core.common.FlowUserMessenger
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.SessionState
import com.jdrms.bulletin.domain.profile.application.SignOutUser
import com.jdrms.bulletin.domain.profile.application.SoftDeleteProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {
    @Test
    fun accountDeletionClearsTheSharedSession() = profileTest {
        val fixture = ProfileFixture()
        val profile = fixture.authenticate()
        val signOut = SignOutUser(fixture.auth)
        val vm = AccountViewModel(
            fixture.session,
            signOut,
            SoftDeleteProfile(fixture.profiles, signOut),
            FlowUserMessenger()
        )
        var completed = false
        vm.deleteProfile { completed = true }
        advanceUntilIdle()
        assertTrue(completed)
        assertIs<SessionState.Unauthenticated>(fixture.session.state.value)
        assertTrue(fixture.profiles.getProfile(profile.id).getOrNull()?.isDeleted == true)
    }

    @Test
    fun signOutFailureRetainsTheAuthenticatedSession() = profileTest {
        val fixture = ProfileFixture()
        fixture.authenticate()
        val failingAuth = object : AuthRepository by fixture.auth {
            override suspend fun signOut(): Result<Unit> = Result.Error(IllegalStateException("sign out failed"))
        }
        val signOut = SignOutUser(failingAuth)
        val messenger = FlowUserMessenger()
        val received = backgroundScope.async { messenger.messages.first() }
        runCurrent()
        val vm = AccountViewModel(
            fixture.session,
            signOut,
            SoftDeleteProfile(fixture.profiles, signOut),
            messenger
        )
        vm.signOut()
        advanceUntilIdle()
        assertEquals("sign out failed", vm.uiState.value.errorMessage)
        assertEquals("sign out failed", received.await())
        assertIs<SessionState.Authenticated>(fixture.session.state.value)
    }

    @Test
    fun deletionFailureKeepsTheProfileAndSession() = profileTest {
        val fixture = ProfileFixture()
        val profile = fixture.authenticate()
        val failingProfiles = object : ProfileRepository by fixture.profiles {
            override suspend fun softDelete(userId: UserId, deletedAt: String): Result<Unit> =
                Result.Error(IllegalStateException("delete failed"))
        }
        val signOut = SignOutUser(fixture.auth)
        val vm = AccountViewModel(
            fixture.session,
            signOut,
            SoftDeleteProfile(failingProfiles, signOut),
            FlowUserMessenger()
        )
        vm.deleteProfile()
        advanceUntilIdle()
        assertEquals("delete failed", vm.uiState.value.errorMessage)
        assertIs<SessionState.Authenticated>(fixture.session.state.value)
        assertEquals(false, fixture.profiles.getProfile(profile.id).getOrThrow()?.isDeleted)
    }
}
