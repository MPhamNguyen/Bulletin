package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.MessageSender
import com.jdrms.bulletin.domain.messages.application.MessageSenderLookupException
import com.jdrms.bulletin.domain.messages.application.MessagingAuthenticationRequiredException
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.profile.application.RestoreAuthenticatedProfile
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class AuthMessageSenderProviderTest {
    private val profile = StudentProfile(UserId("auth-user-id"), StudentEmail("student@example.edu"), "Actual Student")

    @Test
    fun profileIsTranslatedToMessagesOwnedIdentityAndRefreshedAfterAccountChange() = runTest {
        var current: Result<StudentProfile?> = Result.Success(profile)
        val provider = provider { current }
        assertEquals(
            Result.Success(MessageSender(SenderId("auth-user-id"), "Actual Student")),
            provider.getCurrentSender()
        )

        current = Result.Success(profile.copy(id = UserId("second-id"), fullName = "Second Student"))
        assertEquals(
            Result.Success(MessageSender(SenderId("second-id"), "Second Student")),
            provider.getCurrentSender()
        )

        current = Result.Success(null)
        assertIs<MessagingAuthenticationRequiredException>(
            assertIs<Result.Error>(provider.getCurrentSender()).exception
        )
    }

    @Test
    fun failedAuthLookupExposesSafeMessageAndPreservesCause() = runTest {
        val cause = IllegalStateException("Private server detail")
        val result = assertIs<Result.Error>(provider { Result.Error(cause) }.getCurrentSender())
        assertIs<MessageSenderLookupException>(result.exception)
        assertEquals("Unable to verify your messaging identity. Please try again.", result.message)
        assertSame(cause, result.exception.cause)
    }

    @Test
    fun cancellationIsNotConvertedToAnAuthenticationFailure() = runTest {
        val provider = provider { throw CancellationException("Cancelled") }
        assertFailsWith<CancellationException> { provider.getCurrentSender() }
    }

    private fun provider(current: suspend () -> Result<StudentProfile?>): AuthMessageSenderProvider {
        val auth = object : AuthRepository by InMemoryAuthRepository() {
            override suspend fun getCurrentUser(): Result<StudentProfile?> = current()
        }
        return AuthMessageSenderProvider(RestoreAuthenticatedProfile(auth))
    }
}
