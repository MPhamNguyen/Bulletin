package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.AuthenticateUser
import com.jdrms.bulletin.domain.profile.application.ManageProfile
import com.jdrms.bulletin.domain.profile.application.SubmitStudentReview
import com.jdrms.bulletin.domain.profile.application.UpdateStudentProfile
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.Rating
import com.jdrms.bulletin.domain.profile.domain.model.ReviewId
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ReviewDto
import com.jdrms.bulletin.domain.profile.infrastructure.mapper.ProfileMapper
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabaseAuthRepository
import com.jdrms.bulletin.domain.profile.presentation.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
        assertEquals("Full name is required.", (result as Result.Error).exception.message)
    }

    @Test
    fun testValidateRegistrationFailsForEmptyEmail() {
        val result = policy.validateRegistration(
            emailStr = "",
            password = "securePassword123",
            fullName = "Jane Student"
        )
        assertTrue(result.isError())
        assertEquals("Email is required.", (result as Result.Error).exception.message)
    }

    @Test
    fun testValidateRegistrationFailsForInvalidEmail() {
        val result = policy.validateRegistration(
            emailStr = "invalid-email-address",
            password = "securePassword123",
            fullName = "Jane Student"
        )
        assertTrue(result.isError())
        assertEquals("Invalid email address format.", (result as Result.Error).exception.message)
    }

    @Test
    fun testValidateRegistrationFailsForEmptyPassword() {
        val result = policy.validateRegistration(
            emailStr = "student@gmail.com",
            password = "",
            fullName = "Jane Student"
        )
        assertTrue(result.isError())
        assertEquals("Password is required.", (result as Result.Error).exception.message)
    }

    @Test
    fun testValidateRegistrationFailsForShortPassword() {
        val result = policy.validateRegistration(
            emailStr = "student@gmail.com",
            password = "1234567",
            fullName = "Jane Student"
        )
        assertTrue(result.isError())
        assertEquals("Password must be at least 8 characters.", (result as Result.Error).exception.message)
    }

    @Test
    fun testStudentEmailTrimsWhitespaceAndPreservesEquality() {
        val email1 = StudentEmail("  dominic@csulb.edu  ")
        val email2 = StudentEmail("dominic@csulb.edu")
        val emailUpper = StudentEmail("DOMINIC@CSULB.EDU")
        assertEquals("dominic@csulb.edu", email1.value)
        assertEquals(email1, email2)
        assertEquals(email1, emailUpper)
        assertEquals(email1.hashCode(), email2.hashCode())
        assertEquals(email1.hashCode(), emailUpper.hashCode())
    }

    @Test
    fun testAuthRepositoryWithExplicitCredentials() = runTest {
        val profileRepo = InMemoryProfileRepository()
        val authRepo = InMemoryAuthRepository(
            profileRepository = profileRepo,
            initialCredentials = mapOf("dominic.alfonso@student.csulb.edu" to "password123")
        )
        val email = StudentEmail("dominic.alfonso@student.csulb.edu")
        val loginResult = authRepo.login(email, "wrongPassword")
        assertTrue(loginResult.isError())

        val loginCorrect = authRepo.login(email, "password123")
        assertTrue(loginCorrect.isSuccess())
    }

    @Test
    fun testAuthRepositoryWithoutCredentialsFailsByDefault() = runTest {
        val profileRepo = InMemoryProfileRepository()
        val authRepo = InMemoryAuthRepository(profileRepo) // empty credentials by default
        val email = StudentEmail("dominic.alfonso@student.csulb.edu")
        val loginResult = authRepo.login(email, "password123")
        assertTrue(loginResult.isError())
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
    fun testStudentProfileUpdatesEditableDetails() {
        val profile = StudentProfile(
            id = UserId("student_1"),
            email = StudentEmail("student@example.com"),
            fullName = "Original Name"
        )

        val result = profile.updateDetails(
            fullName = "  John Doe  ",
            major = "  Computer Science  ",
            university = "  California State University - Long Beach  ",
            bio = "  Campus seller and student.  "
        )

        assertTrue(result is Result.Success)
        assertEquals("John Doe", result.data.fullName)
        assertEquals("Computer Science", result.data.major)
        assertEquals("California State University - Long Beach", result.data.university)
        assertEquals("Campus seller and student.", result.data.bio)
        assertEquals("Original Name", profile.fullName)
    }

    @Test
    fun testStudentProfileRejectsInvalidEditableDetails() {
        val profile = StudentProfile(
            id = UserId("student_1"),
            email = StudentEmail("student@example.com"),
            fullName = "Original Name"
        )

        val blankName = profile.updateDetails(" ", "Computer Science", "CSULB", "Bio")
        assertTrue(blankName is Result.Error)
        assertEquals("Full name is required.", blankName.exception.message)

        val blankSchool = profile.updateDetails("John Doe", "Computer Science", " ", "Bio")
        assertTrue(blankSchool is Result.Error)
        assertEquals("School is required.", blankSchool.exception.message)

        val longBio = profile.updateDetails(
            "John Doe",
            "Computer Science",
            "CSULB",
            "x".repeat(StudentProfile.MAX_BIO_LENGTH + 1)
        )
        assertTrue(longBio is Result.Error)
        assertEquals("Bio must be 500 characters or fewer.", longBio.exception.message)
    }

    @Test
    fun testUpdateStudentProfilePersistsValidDetails() = runTest {
        val repository = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val profile = StudentProfile(
            id = UserId("student_1"),
            email = StudentEmail("student@example.com"),
            fullName = "Original Name"
        )
        repository.updateProfile(profile)

        val result = UpdateStudentProfile(repository)(
            profile = profile,
            fullName = "John Doe",
            major = "Computer Science",
            university = "CSULB",
            bio = "Student bio"
        )

        assertTrue(result is Result.Success)
        assertEquals("Computer Science", result.data.major)
        val persisted = repository.getProfile(profile.id)
        assertTrue(persisted is Result.Success)
        assertEquals(result.data, persisted.data?.copy(reputation = null))
    }

    @Test
    fun testUpdateStudentProfileDoesNotPersistInvalidDetails() = runTest {
        val repository = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val profile = StudentProfile(
            id = UserId("student_1"),
            email = StudentEmail("student@example.com"),
            fullName = "Original Name"
        )
        repository.updateProfile(profile)

        val result = UpdateStudentProfile(repository)(
            profile = profile,
            fullName = " ",
            major = "Computer Science",
            university = "CSULB",
            bio = "Student bio"
        )

        assertTrue(result is Result.Error)
        val persisted = repository.getProfile(profile.id)
        assertTrue(persisted is Result.Success)
        assertEquals("Original Name", persisted.data?.fullName)
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
            val updateStudentProfile = UpdateStudentProfile(profileRepo)
            val submitStudentReview = SubmitStudentReview(profileRepo, policy)

            val viewModel = ProfileViewModel(
                authenticateUser = authenticateUser,
                verifyStudentEmail = verifyStudentEmail,
                manageProfile = manageProfile,
                updateStudentProfile = updateStudentProfile,
                submitStudentReview = submitStudentReview
            )
            advanceUntilIdle()

            // Blank first name check
            viewModel.createAccount("", "Doe", "test@example.com", "password123")
            advanceUntilIdle()
            assertEquals("First name is required.", viewModel.uiState.value.errorMessage)

            // Blank last name check
            viewModel.createAccount("John", "", "test@example.com", "password123")
            advanceUntilIdle()
            assertEquals("Last name is required.", viewModel.uiState.value.errorMessage)

            // Blank email check
            viewModel.createAccount("John", "Doe", "", "password123")
            advanceUntilIdle()
            assertEquals("Email is required.", viewModel.uiState.value.errorMessage)

            // Invalid email check
            viewModel.createAccount("John", "Doe", "notanemail", "password123")
            advanceUntilIdle()
            assertEquals("Invalid email address format.", viewModel.uiState.value.errorMessage)

            // Empty password check
            viewModel.createAccount("John", "Doe", "test@example.com", "")
            advanceUntilIdle()
            assertEquals("Password is required.", viewModel.uiState.value.errorMessage)

            // Short password check
            viewModel.createAccount("John", "Doe", "test@example.com", "123")
            advanceUntilIdle()
            assertEquals("Password must be at least 8 characters.", viewModel.uiState.value.errorMessage)

            // Successful account creation
            viewModel.createAccount("John", "Doe", "john.doe@example.com", "password123")
            runCurrent()

            val state = viewModel.uiState.value
            assertTrue(state.isAccountCreated)
            assertEquals("Account created successfully!", state.successMessage)
            assertNull(state.errorMessage)
            assertNotNull(state.profile)
            assertEquals("John Doe", state.profile.fullName)
            assertEquals("John Doe", state.profileDraft.fullName)

            advanceTimeBy(ProfileViewModel.FLASH_NOTIFICATION_DURATION_MILLIS)
            runCurrent()
            assertNull(viewModel.uiState.value.errorMessage)
            assertNull(viewModel.uiState.value.successMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testProfileViewModelUpdatesAndResetsProfileDraft() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val authRepo = InMemoryAuthRepository(profileRepo)
            val viewModel = ProfileViewModel(
                authenticateUser = AuthenticateUser(authRepo, policy),
                verifyStudentEmail = VerifyStudentEmail(authRepo),
                manageProfile = ManageProfile(profileRepo),
                updateStudentProfile = UpdateStudentProfile(profileRepo),
                submitStudentReview = SubmitStudentReview(profileRepo, policy)
            )
            advanceUntilIdle()
            viewModel.createAccount("John", "Doe", "john.doe@example.com", "password123")
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.isProfileModified)

            viewModel.onProfileDraftChanged(
                viewModel.uiState.value.profileDraft.copy(
                    fullName = "Jane Doe",
                    major = "Computer Science",
                    university = "California State University - Long Beach",
                    bio = "Campus student"
                )
            )
            assertTrue(viewModel.uiState.value.isProfileModified)

            viewModel.updateProfileDetails()
            runCurrent()

            val updatedState = viewModel.uiState.value
            assertFalse(updatedState.isProfileModified)
            assertEquals("Jane Doe", updatedState.profile?.fullName)
            assertEquals("Computer Science", updatedState.profile?.major)
            assertEquals("Profile updated", updatedState.successMessage)

            advanceTimeBy(ProfileViewModel.FLASH_NOTIFICATION_DURATION_MILLIS - 1)
            runCurrent()
            assertEquals("Profile updated", viewModel.uiState.value.successMessage)
            advanceTimeBy(1)
            runCurrent()
            assertNull(viewModel.uiState.value.successMessage)

            viewModel.onProfileDraftChanged(updatedState.profileDraft.copy(fullName = "Unsaved Name"))
            assertTrue(viewModel.uiState.value.isProfileModified)

            viewModel.resetProfileDraft()
            assertFalse(viewModel.uiState.value.isProfileModified)
            assertEquals("Jane Doe", viewModel.uiState.value.profileDraft.fullName)

            viewModel.onProfileDraftChanged(viewModel.uiState.value.profileDraft.copy(fullName = " "))
            assertTrue(viewModel.uiState.value.isProfileModified)
            viewModel.updateProfileDetails()
            advanceUntilIdle()
            assertEquals("Full name is required.", viewModel.uiState.value.errorMessage)
            assertEquals("Jane Doe", viewModel.uiState.value.profile?.fullName)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testProfileViewModelLoginValidation() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
        try {
            val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
            val authRepo = InMemoryAuthRepository(profileRepo)
            val authenticateUser = AuthenticateUser(authRepo, policy)
            val verifyStudentEmail = VerifyStudentEmail(authRepo)
            val manageProfile = ManageProfile(profileRepo)
            val updateStudentProfile = UpdateStudentProfile(profileRepo)
            val submitStudentReview = SubmitStudentReview(profileRepo, policy)

            val viewModel = ProfileViewModel(
                authenticateUser = authenticateUser,
                verifyStudentEmail = verifyStudentEmail,
                manageProfile = manageProfile,
                updateStudentProfile = updateStudentProfile,
                submitStudentReview = submitStudentReview
            )
            advanceUntilIdle()

            // Empty email check
            viewModel.login("", "password123")
            advanceUntilIdle()
            assertEquals("Email is required.", viewModel.uiState.value.errorMessage)

            // Invalid email check
            viewModel.login("invalid-email", "password123")
            advanceUntilIdle()
            assertEquals("Invalid email address format.", viewModel.uiState.value.errorMessage)

            // Empty password check
            viewModel.login("john@example.com", "")
            advanceUntilIdle()
            assertEquals("Password is required.", viewModel.uiState.value.errorMessage)
        } finally {
            Dispatchers.resetMain()
        }
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
    fun testAppContainerRejectsInMemoryFallbackWhenDisabled() {
        val container = com.jdrms.bulletin.app.di.AppContainer(
            supabaseConfig = com.jdrms.bulletin.core.network.SupabaseConfig(isConnected = false),
            isInspectionMode = false,
            allowInMemoryFallback = false
        )

        assertFailsWith<IllegalStateException> {
            container.profileRepository
        }

        assertFailsWith<IllegalStateException> {
            container.authRepository
        }
    }

    @Test
    fun testProfileRepositoryGetProfileReturnsResult() = runTest {
        val repo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val notFoundResult = repo.getProfile(UserId("nonexistent_user"))
        assertTrue(notFoundResult is Result.Success)
        assertNull(notFoundResult.data)

        val newProfile = com.jdrms.bulletin.domain.profile.domain.model.StudentProfile(
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
    fun testPolicyValidationMethods() {
        val loginResult = policy.validateLogin("invalid-email", "pass")
        assertTrue(loginResult.isError())
        assertEquals("Invalid email address format.", (loginResult as Result.Error).exception.message)

        val regFirstNameResult = policy.validateRegistration("", "Last", "test@csulb.edu", "password123")
        assertTrue(regFirstNameResult.isError())
        assertEquals("First name is required.", (regFirstNameResult as Result.Error).exception.message)

        val regLastNameResult = policy.validateRegistration("First", "", "test@csulb.edu", "password123")
        assertTrue(regLastNameResult.isError())
        assertEquals("Last name is required.", (regLastNameResult as Result.Error).exception.message)
    }
}
