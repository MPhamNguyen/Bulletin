package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.CreateListingErrorMessages
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthListingSellerProviderTest {

    @Test
    fun currentProfileIsTranslatedToListingSellerIdentity() = kotlinx.coroutines.test.runTest {
        val provider = AuthListingSellerProvider(FakeAuthRepository(Result.Success(testProfile())))

        val result = provider.getCurrentSeller()

        assertEquals(Result.Success(ListingSeller(SellerId("user-uuid"), "Jane Student")), result)
    }

    @Test
    fun missingCurrentProfileReturnsActionableAuthenticationError() = kotlinx.coroutines.test.runTest {
        val provider = AuthListingSellerProvider(FakeAuthRepository(Result.Success(null)))

        val result = provider.getCurrentSeller()

        assertTrue(result.isError())
        assertEquals(
            CreateListingErrorMessages.AUTHENTICATION_REQUIRED,
            (result as Result.Error).message
        )
    }

    @Test
    fun providerFailureDoesNotExposeTechnicalError() = kotlinx.coroutines.test.runTest {
        val provider = AuthListingSellerProvider(
            FakeAuthRepository(Result.Error(IllegalStateException("JWT expired: token details")))
        )

        val result = provider.getCurrentSeller()

        assertTrue(result.isError())
        assertEquals(
            CreateListingErrorMessages.GENERIC_FAILURE,
            (result as Result.Error).message
        )
    }

    private fun testProfile(): StudentProfile {
        return StudentProfile(
            id = UserId("user-uuid"),
            email = StudentEmail("jane.student@example.edu"),
            fullName = "Jane Student"
        )
    }

    private class FakeAuthRepository(
        private val currentUserResult: Result<StudentProfile?>
    ) : AuthRepository {
        override suspend fun getCurrentUser(): Result<StudentProfile?> = currentUserResult

        override suspend fun login(email: StudentEmail, password: String): Result<StudentProfile> {
            return Result.Error(UnsupportedOperationException())
        }

        override suspend fun register(
            email: StudentEmail,
            password: String,
            fullName: String,
            university: String
        ): Result<StudentProfile> {
            return Result.Error(UnsupportedOperationException())
        }

        override suspend fun verifyEmail(email: StudentEmail, code: String): Result<Boolean> {
            return Result.Error(UnsupportedOperationException())
        }

        override suspend fun signOut(): Result<Unit> {
            return Result.Error(UnsupportedOperationException())
        }
    }
}
