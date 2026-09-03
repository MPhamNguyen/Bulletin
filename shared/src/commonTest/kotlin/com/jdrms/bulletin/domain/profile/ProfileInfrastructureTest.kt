package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.Rating
import com.jdrms.bulletin.domain.profile.domain.model.ReviewId
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ReviewDto
import com.jdrms.bulletin.domain.profile.infrastructure.mapper.ProfileMapper
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabaseAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabaseProfileRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileInfrastructureTest {

    @Test
    fun testMapperScoreClamping() {
        val dto = ReviewDto(
            id = "dto_r1",
            reviewerId = "u1",
            reviewerName = "Sean",
            revieweeId = "u2",
            score = 10,
            comment = "Great"
        )
        val domain = ProfileMapper.toDomain(dto)
        assertEquals(5, domain.rating.score)
    }

    @Test
    fun testProfileMapperPreservesMajor() {
        val profile = StudentProfile(
            id = UserId("student_1"),
            email = StudentEmail("student@example.com"),
            fullName = "John Doe",
            major = "Computer Science"
        )

        val dto = ProfileMapper.toDto(profile)
        assertEquals("Computer Science", dto.major)
        assertEquals(profile, ProfileMapper.toDomain(dto))
    }

    @Test
    fun testProfileRepositorySubmitAndGetReputation() = runTest {
        val repo = InMemoryProfileRepository(
            initialProfiles = emptyMap(),
            initialReviews = emptyMap()
        )
        val target = UserId("target_student")
        val review = StudentReview(
            id = ReviewId("rev_test"),
            reviewerId = "peer_1",
            reviewerName = "Peer Reviewer",
            revieweeId = target,
            rating = Rating(5),
            comment = "Smooth campus transaction!"
        )

        val submitResult = repo.submitReview(target, review)
        assertTrue(submitResult.isSuccess())

        val rep = repo.getReputation(target)
        assertEquals(5.0, rep.averageRating)
        assertEquals(1, rep.totalReviews)
        assertEquals("Smooth campus transaction!", rep.reviews.first().comment)
    }

    @Test
    fun testProfileRepositoryGetProfileReturnsResult() = runTest {
        val repo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val notFoundResult = repo.getProfile(UserId("nonexistent_user"))
        assertTrue(notFoundResult is Result.Success)
        assertNull(notFoundResult.data)

        val newProfile = StudentProfile(
            id = UserId("u_100"),
            email = StudentEmail("test@csulb.edu"),
            fullName = "Test User"
        )
        repo.updateProfile(newProfile)

        val foundResult = repo.getProfile(UserId("u_100"))
        assertTrue(foundResult is Result.Success)
        assertEquals("Test User", foundResult.data?.fullName)
    }

    @Test
    fun testAuthRepositoryWithExplicitCredentials() = runTest {
        val profileRepo = InMemoryProfileRepository()
        val authRepo = InMemoryAuthRepository(
            profileRepository = profileRepo,
            initialCredentials = mapOf("dominic.alfonso@student.csulb.edu" to "password123")
        )
        val email = StudentEmail("dominic.alfonso@student.csulb.edu")
        val loginWrongPassword = authRepo.login(email, "wrongPassword")
        assertTrue(loginWrongPassword.isError())
        assertEquals(
            "Incorrect password. Please try again.",
            (loginWrongPassword as Result.Error).exception.message
        )

        val unknownEmail = StudentEmail("unknown@csulb.edu")
        val loginUnknownEmail = authRepo.login(unknownEmail, "password123")
        assertTrue(loginUnknownEmail.isError())
        assertEquals(
            "Account not found. Please check your email or create an account.",
            (loginUnknownEmail as Result.Error).exception.message
        )

        val loginCorrect = authRepo.login(email, "password123")
        assertTrue(loginCorrect.isSuccess())
    }

    @Test
    fun testAuthRepositoryWithoutCredentialsFailsByDefault() = runTest {
        val profileRepo = InMemoryProfileRepository()
        val authRepo = InMemoryAuthRepository(
            profileRepository = profileRepo,
            initialCredentials = emptyMap()
        )
        val email = StudentEmail("dominic.alfonso@student.csulb.edu")
        val loginResult = authRepo.login(email, "password123")
        assertTrue(loginResult.isError())
        assertEquals(
            "Account not found. Please check your email or create an account.",
            (loginResult as Result.Error).exception.message
        )
    }

    @Test
    fun testInMemoryAuthRepositoryRegisterAndLogin() = runTest {
        val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val authRepo = InMemoryAuthRepository(profileRepo)

        val email = StudentEmail("newuser@gmail.com")
        val registerResult = authRepo.register(
            email = email,
            password = "mypassword123",
            fullName = "New User",
            university = "CSU Long Beach"
        )
        assertTrue(registerResult is Result.Success)

        val createdProfile = registerResult.data
        assertEquals("New User", createdProfile.fullName)
        assertEquals(email, createdProfile.email)
        assertEquals(createdProfile, (authRepo.getCurrentUser() as Result.Success).data)

        // Login with correct password
        val loginSuccess = authRepo.login(email, "mypassword123")
        assertTrue(loginSuccess is Result.Success)
        assertEquals(createdProfile.id, loginSuccess.data.id)

        // Login with wrong password
        val loginWrongPassword = authRepo.login(email, "wrongPassword")
        assertTrue(loginWrongPassword.isError())

        // Duplicate registration fails
        val duplicateRegister = authRepo.register(
            email = email,
            password = "anotherPassword",
            fullName = "Duplicate User"
        )
        assertTrue(duplicateRegister.isError())

        assertTrue(authRepo.signOut().isSuccess())
        assertNull((authRepo.getCurrentUser() as Result.Success).data)
    }

    @Test
    fun testSupabaseAuthRepositoryErrorMapping() {
        val userExistsError = Exception(
            "user_already_exists (User already registered: user_already_exists)\n" +
                "URL: https://example.supabase.co/auth/v1/signup\n" +
                "Headers: [Authorization=[Bearer token123], apikey=[key123]]"
        )
        assertEquals(
            "An account with this email address already exists. Please log in instead.",
            SupabaseAuthRepository.mapAuthErrorMessage(userExistsError)
        )

        val rateLimitError = Exception("over_email_send_rate_limit (email rate limit exceeded)")
        assertEquals(
            "Too many signup attempts. Please wait a few minutes before trying again.",
            SupabaseAuthRepository.mapAuthErrorMessage(rateLimitError)
        )

        val invalidCredentialsError = Exception("invalid_credentials (Invalid login credentials)")
        assertEquals(
            "Invalid email or password. Please try again.",
            SupabaseAuthRepository.mapAuthErrorMessage(invalidCredentialsError)
        )

        val invalidEmailError = Exception("email_address_invalid (Email address is invalid)")
        assertEquals(
            "Invalid email address. Please use a valid university or personal email domain.",
            SupabaseAuthRepository.mapAuthErrorMessage(invalidEmailError)
        )

        val connectionError = Exception("Failed to connect to host (timeout)")
        assertEquals(
            "Unable to connect to server. Please check your internet connection.",
            SupabaseAuthRepository.mapAuthErrorMessage(connectionError)
        )
    }

    @Test
    fun testSupabaseProfileRepositoryErrorMapping() {
        val schemaCacheError = Exception(
            "Could not find the table `public.profiles` in the schema cache.\n\n" +
                "URL:\n" +
                "https://qullzprtorshyqkxuvac.supabase.co/rest/v1/profiles?id=eq.current_student&select=%2A\n\n" +
                "Headers:\n\n" +
                "* Authorization: Bearer [redacted]\n" +
                "* Content-Type: application/json\n" +
                "* Prefer:\n" +
                "* Accept-Profile: public\n" +
                "* apikey: [redacted]\n" +
                "* X-Client-Info: supabase-kt/3.1.4\n" +
                "* Accept: application/json\n" +
                "* Accept-Charset: UTF-8\n\n" +
                "HTTP method: GET"
        )
        assertEquals(
            "Database table not found. Please verify your Supabase schema setup.",
            SupabaseProfileRepository.mapProfileErrorMessage(schemaCacheError)
        )

        val timeoutError = Exception(
            "Request timeout has expired " +
                "[url=https://qullzprtorshyqkxuvac.supabase.co/rest/v1/profiles?id=eq.current_student&select=%2A, " +
                "request_timeout=10000 ms]"
        )
        assertEquals(
            "Unable to connect to server. Please check your internet connection.",
            SupabaseProfileRepository.mapProfileErrorMessage(timeoutError)
        )

        val unauthorizedError = Exception("invalid api key provided in apikey header")
        assertEquals(
            "Unauthorized database request. Please check your Supabase API credentials.",
            SupabaseProfileRepository.mapProfileErrorMessage(unauthorizedError)
        )

        val rlsError = Exception("new row violates row-level security policy for table profiles")
        assertEquals(
            "Database permission denied. Please check your Supabase RLS policies.",
            SupabaseProfileRepository.mapProfileErrorMessage(rlsError)
        )

        val customFirstLineError = Exception("User profile is temporarily locked by administrator.\nURL: https://...\n")
        assertEquals(
            "User profile is temporarily locked by administrator.",
            SupabaseProfileRepository.mapProfileErrorMessage(customFirstLineError)
        )

        val uuidSyntaxError = Exception("invalid input syntax for type uuid: \"current_student\"")
        assertEquals(
            "Invalid user identifier format.",
            SupabaseProfileRepository.mapProfileErrorMessage(uuidSyntaxError)
        )
    }

    @Test
    fun testUuidValidation() {
        assertTrue(SupabaseProfileRepository.isValidUuid("c3a81234-5678-4abc-9def-123456789abc"))
        assertTrue(SupabaseProfileRepository.isValidUuid("00000000-0000-0000-0000-000000000000"))
        assertFalse(SupabaseProfileRepository.isValidUuid("current_student"))
        assertFalse(SupabaseProfileRepository.isValidUuid("user_101"))
        assertFalse(SupabaseProfileRepository.isValidUuid(""))
        assertFalse(SupabaseProfileRepository.isValidUuid("not-a-uuid-at-all"))
    }
}
