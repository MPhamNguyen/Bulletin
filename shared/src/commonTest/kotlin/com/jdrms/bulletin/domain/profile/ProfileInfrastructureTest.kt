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

        val uuidError = Exception(
            "invalid input syntax for type uuid: \"current_student\"\n\n" +
                "URL:\nhttps://example.supabase.co/rest/v1/profiles?id=eq.current_student&select=%2A\n\n" +
                "Headers: [Authorization=[Bearer token123], apikey=[key123]]\n\n" +
                "Http Method: GET"
        )
        assertEquals(
            "The requested user account was not found.",
            SupabaseProfileRepository.mapProfileErrorMessage(uuidError)
        )

        val genericPostgrestDump = Exception(
            "PGRST200: column not found\n\n" +
                "URL:\nhttps://example.supabase.co/rest/v1/profiles?id=eq.123\n\n" +
                "Headers: [apikey=[key123]]"
        )
        assertEquals(
            "Unable to complete profile operation. Please try again.",
            SupabaseProfileRepository.mapProfileErrorMessage(genericPostgrestDump)
        )
    }
}
