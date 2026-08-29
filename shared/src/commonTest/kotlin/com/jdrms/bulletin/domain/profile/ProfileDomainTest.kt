package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.AuthenticateUser
import com.jdrms.bulletin.domain.profile.application.ManageProfile
import com.jdrms.bulletin.domain.profile.application.SubmitStudentReview
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.Rating
import com.jdrms.bulletin.domain.profile.domain.model.ReviewId
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ReviewDto
import com.jdrms.bulletin.domain.profile.infrastructure.mapper.ProfileMapper
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import com.jdrms.bulletin.domain.profile.presentation.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileDomainTest {

    private val policy = ProfileValidationPolicy()

    @Test
    fun testValidUniversityEmail() {
        val email = StudentEmail("dominic@csulb.edu")
        assertTrue(email.isUniversityEmail)
        assertEquals("dominic@csulb.edu", email.value)
    }

    @Test
    fun testValidNonUniversityEmailsMatchRegex() {
        val gmail = StudentEmail("jane.doe@gmail.com")
        assertFalse(gmail.isUniversityEmail)
        assertEquals("jane.doe@gmail.com", gmail.value)

        assertTrue(StudentEmail.isValid("user_name+tag@sub.domain.org"))
        assertTrue(StudentEmail.isValid("simple@example.com"))
    }

    @Test
    fun testInvalidEmailThrowsException() {
        assertFailsWith<IllegalArgumentException> {
            StudentEmail("not-an-email")
        }
        assertFailsWith<IllegalArgumentException> {
            StudentEmail("user@")
        }
        assertFailsWith<IllegalArgumentException> {
            StudentEmail("@domain.com")
        }
        assertFailsWith<IllegalArgumentException> {
            StudentEmail("user@domain")
        }
    }

    @Test
    fun testValidateRegistrationSuccess() {
        val result = policy.validateRegistration(
            emailStr = "student@gmail.com",
            password = "securePassword123",
            fullName = "Jane Student"
        )
        assertTrue(result.isSuccess())
    }

    @Test
    fun testValidateRegistrationFailsForBlankName() {
        val result = policy.validateRegistration(
            emailStr = "student@gmail.com",
            password = "securePassword123",
            fullName = "   "
        )
        assertTrue(result.isError())
    }

    @Test
    fun testValidateRegistrationFailsForInvalidEmail() {
        val result = policy.validateRegistration(
            emailStr = "invalid-email-address",
            password = "securePassword123",
            fullName = "Jane Student"
        )
        assertTrue(result.isError())
    }

    @Test
    fun testValidateRegistrationFailsForShortPassword() {
        val result = policy.validateRegistration(
            emailStr = "student@gmail.com",
            password = "12345",
            fullName = "Jane Student"
        )
        assertTrue(result.isError())
    }

    @Test
    fun testNonEduEmailFailsUniversityValidation() {
        val result = policy.validateUniversityRegistration("user@gmail.com")
        assertTrue(result.isError())
    }

    @Test
    fun testEduEmailPassesUniversityValidation() {
        val result = policy.validateUniversityRegistration("student@csulb.edu")
        assertTrue(result.isSuccess())
    }

    @Test
    fun testRatingBoundaries() {
        val rating = Rating(5)
        assertEquals(5, rating.score)

        assertFailsWith<IllegalArgumentException> { Rating(6) }
        assertFailsWith<IllegalArgumentException> { Rating(0) }
    }

    @Test
    fun testSelfReviewValidationFails() {
        val review = StudentReview(
            id = ReviewId("r1"),
            reviewerId = "user_101",
            reviewerName = "Dominic",
            revieweeId = UserId("user_101"),
            rating = Rating(5),
            comment = "Self review"
        )
        val result = policy.validateNewReview(review)
        assertTrue(result.isError())
    }

    @Test
    fun testCalculateReputationAverage() {
        val user = UserId("user_1")
        val reviews = listOf(
            StudentReview(ReviewId("r1"), "u2", "Sean", user, Rating(5), "Great!"),
            StudentReview(ReviewId("r2"), "u3", "Jacob", user, Rating(4), "Good!")
        )
        val rep = policy.calculateReputation(user, reviews)
        assertEquals(4.5, rep.averageRating)
        assertEquals(2, rep.totalReviews)
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
    }

    @Test
    fun testAuthenticateUserUseCaseRegisterAndLogin() = runTest {
        val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val authRepo = InMemoryAuthRepository(profileRepo)
        val authenticateUser = AuthenticateUser(authRepo, policy)

        val email = StudentEmail("student1@example.com")
        val registerResult = authenticateUser.register(
            email = email,
            password = "validPassword123",
            fullName = "First Last"
        )
        assertTrue(registerResult.isSuccess())

        // Invalid registration fails in use case
        val shortPasswordResult = authenticateUser.register(
            email = StudentEmail("student2@example.com"),
            password = "123",
            fullName = "Short Pass"
        )
        assertTrue(shortPasswordResult.isError())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testProfileViewModelCreateAccountAndValidation() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val authRepo = InMemoryAuthRepository(profileRepo)
            val authenticateUser = AuthenticateUser(authRepo, policy)
            val verifyStudentEmail = VerifyStudentEmail(authRepo)
            val manageProfile = ManageProfile(profileRepo)
            val submitStudentReview = SubmitStudentReview(profileRepo, policy)

            val viewModel = ProfileViewModel(
                authenticateUser = authenticateUser,
                verifyStudentEmail = verifyStudentEmail,
                manageProfile = manageProfile,
                submitStudentReview = submitStudentReview
            )
            advanceUntilIdle()

            // Blank name check
            viewModel.createAccount("", "", "test@example.com", "password123")
            assertEquals("First name and last name are required.", viewModel.uiState.value.errorMessage)

            // Invalid email check
            viewModel.createAccount("John", "Doe", "notanemail", "password123")
            assertEquals("Invalid email address format.", viewModel.uiState.value.errorMessage)

            // Short password check
            viewModel.createAccount("John", "Doe", "test@example.com", "123")
            assertEquals("Password must be at least 6 characters.", viewModel.uiState.value.errorMessage)

            // Successful account creation
            viewModel.createAccount("John", "Doe", "john.doe@example.com", "password123")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isAccountCreated)
            assertEquals("Account created successfully!", state.successMessage)
            assertNull(state.errorMessage)
            assertNotNull(state.profile)
            assertEquals("John Doe", state.profile.fullName)

            // Clear messages
            viewModel.clearMessages()
            assertNull(viewModel.uiState.value.errorMessage)
            assertNull(viewModel.uiState.value.successMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
