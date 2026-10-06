package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationOutcome
import com.jdrms.bulletin.domain.profile.domain.model.Rating
import com.jdrms.bulletin.domain.profile.domain.model.ReviewId
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ProfileDto
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ReviewDto
import com.jdrms.bulletin.domain.profile.infrastructure.mapper.ProfileMapper
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabaseAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabaseProfileRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
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
    fun testProfileMapperAcceptsNullOptionalSupabaseFields() {
        val dto = Json.decodeFromString<ProfileDto>(
            """
            {
                "id":"c3a81234-5678-4abc-9def-123456789abc",
                "email":"student@csulb.edu",
                "full_name":null,
                "major":null,
                "university":null,
                "bio":null,
                "is_verified":null
            }
            """.trimIndent()
        )

        val profile = ProfileMapper.toDomain(dto)

        assertEquals("Student", profile.fullName)
        assertEquals("", profile.major)
        assertEquals("CSU Long Beach", profile.university)
        assertEquals("", profile.bio)
        assertFalse(profile.isVerified)
    }

    @Test
    fun testProfileMapperHandlesNullAndBlankFields() {
        val dto = ProfileDto(
            id = "student_1",
            email = "student@example.com",
            fullName = "  John Doe  ",
            major = "   ",
            university = "  CSULB  ",
            bio = null
        )

        val domain = ProfileMapper.toDomain(dto)
        assertEquals("John Doe", domain.fullName)
        assertEquals("", domain.major)
        assertEquals("", domain.bio)
        assertEquals("CSULB", domain.university)
    }

    @Test
    fun testProfileDtoDeserializationWithNullMajorAndBio() {
        val json = """
            {
                "id": "student_1",
                "email": "student@example.com",
                "full_name": "John Doe",
                "major": null,
                "university": "CSULB",
                "bio": null,
                "is_verified": true
            }
        """.trimIndent()

        val jsonParser = Json { ignoreUnknownKeys = true }
        val dto = jsonParser.decodeFromString<ProfileDto>(json)
        assertNull(dto.major)
        assertNull(dto.bio)
        assertEquals("John Doe", dto.fullName)

        val domain = ProfileMapper.toDomain(dto)
        assertEquals("", domain.major)
        assertEquals("", domain.bio)
        assertTrue(domain.isVerified)
    }

    @Test
    fun testProfileMapperToUpdateDtoOnlyIncludesEditableFields() {
        val profile = StudentProfile(
            id = UserId("student_1"),
            email = StudentEmail("student@example.com"),
            fullName = "John Doe",
            major = "Computer Science",
            graduationDate = "Class of 2026",
            university = "CSULB",
            bio = "Campus student",
            avatarUrl = "https://example.com/avatar.png",
            deleteAtMillis = 1_791_201_600_000L
        )

        val updateDto = ProfileMapper.toUpdateDto(profile)
        assertEquals("John Doe", updateDto.fullName)
        assertEquals("Computer Science", updateDto.major)
        assertEquals("CSULB", updateDto.university)
        assertEquals("Campus student", updateDto.bio)

        val serializedFields = Json.encodeToJsonElement(
            serializer = com.jdrms.bulletin.domain.profile.infrastructure.dto.ProfileUpdateDto.serializer(),
            value = updateDto
        ).jsonObject.keys
        assertEquals(
            setOf("full_name", "major", "university", "bio", "deleted_at"),
            serializedFields
        )
        assertFalse("delete_at" in serializedFields)
        assertFalse("graduation_date" in serializedFields)
        assertFalse("avatar_url" in serializedFields)
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
        val authRepo = InMemoryAuthRepository(profileRepo, testVerificationCode = "123456")

        val email = StudentEmail("newuser@gmail.com")
        val registerResult = authRepo.register(
            email = email,
            password = "mypassword123",
            fullName = "New User",
            university = "CSU Long Beach"
        )
        assertTrue(registerResult is Result.Success)

        assertTrue(authRepo.login(email, "mypassword123").isError())
        assertNull((authRepo.getCurrentUser() as Result.Success).data)
        val verified = VerifyStudentEmail(authRepo)(email, "123456")
        assertTrue(verified is Result.Success)
        val createdProfile = assertIs<EmailVerificationOutcome.ProfileAvailable>(verified.data).profile
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
    fun testSupabaseAuthRepositoryRecognizesExistingUserErrors() {
        assertTrue(SupabaseAuthRepository.isExistingUserError(Exception("user_already_exists")))
        assertTrue(SupabaseAuthRepository.isExistingUserError(Exception("User already registered")))
        assertFalse(SupabaseAuthRepository.isExistingUserError(Exception("Invalid login credentials")))
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

    @Test
    fun testProfileMapperHandlesDeletedAtTimestamp() {
        val dto = Json.decodeFromString<ProfileDto>(
            """
            {
                "id": "student_deleted",
                "email": "deleted@example.com",
                "full_name": "Deleted Student",
                "deleted_at": "2026-10-05T12:00:00Z"
            }
            """.trimIndent()
        )
        val domain = ProfileMapper.toDomain(dto)
        assertTrue(domain.isDeleted)
        assertEquals(1791201600000L, domain.deleteAtMillis)

        val mappedBackDto = ProfileMapper.toDto(domain)
        assertEquals("2026-10-05T12:00:00Z", mappedBackDto.deletedAt)

        val updateDto = ProfileMapper.toUpdateDto(domain)
        assertEquals("2026-10-05T12:00:00Z", updateDto.deletedAt)
        assertEquals("deleted_at", SupabaseProfileRepository.DELETED_AT_COLUMN)
    }

    @Test
    fun testSupabaseProfileRepositoryRejectsUpdateWithNoAffectedRows() {
        assertFailsWith<IllegalStateException> {
            SupabaseProfileRepository.requireAffectedProfileRows(emptyList())
        }

        SupabaseProfileRepository.requireAffectedProfileRows(
            listOf(
                ProfileDto(
                    id = "student_del",
                    email = "deleted@example.com"
                )
            )
        )
    }

    @Test
    fun testInMemoryProfileRepositoryDeleteProfile() = runTest {
        val repo = InMemoryProfileRepository(
            initialProfiles = emptyMap(),
            initialReviews = emptyMap()
        )
        val profile = StudentProfile(
            id = UserId("student_del"),
            email = StudentEmail("del@csulb.edu"),
            fullName = "To Delete"
        )
        repo.updateProfile(profile)

        val deleteTimestamp = 1_700_000_000_000L
        val deleteResult = repo.deleteProfile(UserId("student_del"), deleteTimestamp)
        assertTrue(deleteResult.isSuccess())

        val fetched = repo.getProfile(UserId("student_del"))
        assertTrue(fetched is Result.Success)
        val fetchedData = fetched.data
        assertNotNull(fetchedData)
        assertTrue(fetchedData.isDeleted)
        assertEquals(deleteTimestamp, fetchedData.deleteAtMillis)

        // Nonexistent profile deletion returns error
        val nonExistentDelete = repo.deleteProfile(UserId("unknown_student"), deleteTimestamp)
        assertTrue(nonExistentDelete.isError())
    }

    @Test
    fun testInMemoryAuthRepositoryRejectsLoginForDeletedAccount() = runTest {
        val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val authRepo = InMemoryAuthRepository(profileRepo, testVerificationCode = "123456")

        val email = StudentEmail("active@example.com")
        val registerResult = authRepo.register(
            email = email,
            password = "password123",
            fullName = "Active User"
        )
        assertTrue(registerResult is Result.Success)
        VerifyStudentEmail(authRepo)(email, "123456")
        val deletedUserId = assertNotNull(
            assertIs<Result.Success<StudentProfile?>>(authRepo.getCurrentUser()).data
        ).id

        // Soft delete the profile
        val deleteResult = profileRepo.deleteProfile(
            deletedUserId,
            1_700_000_000_000L
        )
        assertTrue(deleteResult.isSuccess())

        // Login fails because account is deleted
        val loginResult = authRepo.login(email, "password123")
        assertTrue(loginResult.isError())
        assertEquals("Account has been deleted.", (loginResult as Result.Error).exception.message)
    }

    @Test
    fun testInMemoryProfileRepositoryUpdateProfileReplacesOldEntryWithSameEmail() = runTest {
        val repo = InMemoryProfileRepository(
            initialProfiles = emptyMap(),
            initialReviews = emptyMap()
        )
        val email = StudentEmail("reused@csulb.edu")
        val oldProfile = StudentProfile(
            id = UserId("old_id"),
            email = email,
            fullName = "Old User",
            deleteAtMillis = 1_700_000_000_000L
        )
        repo.updateProfile(oldProfile)
        val initialFetched = repo.getProfile(UserId("old_id"))
        assertTrue(initialFetched is Result.Success && initialFetched.data?.isDeleted == true)

        val newProfile = StudentProfile(
            id = UserId("new_id"),
            email = email,
            fullName = "New User",
            deleteAtMillis = null
        )
        repo.updateProfile(newProfile)

        val oldFetched = repo.getProfile(UserId("old_id"))
        assertTrue(oldFetched is Result.Success)
        assertNull(oldFetched.data)

        val newFetched = repo.getProfile(UserId("new_id"))
        assertTrue(newFetched is Result.Success)
        assertNotNull(newFetched.data)
        val newFetchedData = assertNotNull(newFetched.data)
        assertFalse(newFetchedData.isDeleted)
        assertEquals("New User", newFetchedData.fullName)
    }

    @Test
    fun testInMemoryAuthRepositoryAllowsReRegistrationForSoftDeletedAccount() = runTest {
        val profileRepo = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val authRepo = InMemoryAuthRepository(profileRepo, testVerificationCode = "123456")

        val email = StudentEmail("recycle@example.com")
        val reg1 = authRepo.register(email, "pw1", "User One")
        assertTrue(reg1 is Result.Success)

        // Soft delete profile and sign out
        VerifyStudentEmail(authRepo)(email, "123456")
        val oldUserId = assertNotNull(
            assertIs<Result.Success<StudentProfile?>>(authRepo.getCurrentUser()).data
        ).id
        profileRepo.deleteProfile(oldUserId, 1_700_000_000_000L)
        authRepo.signOut()

        // Re-registration with same email succeeds
        val reg2 = authRepo.register(email, "pw2", "User Two")
        assertTrue(reg2 is Result.Success)
        VerifyStudentEmail(authRepo)(email, "123456")
        val reg2Data = assertNotNull(
            assertIs<Result.Success<StudentProfile?>>(authRepo.getCurrentUser()).data
        )
        assertEquals("User Two", reg2Data.fullName)
        assertFalse(reg2Data.isDeleted)

        val current = authRepo.getCurrentUser()
        assertTrue(current is Result.Success)
        assertEquals("User Two", current.data?.fullName)
        assertFalse(current.data?.isDeleted == true)
    }
}
