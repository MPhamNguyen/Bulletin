package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.ProfilePhoto
import com.jdrms.bulletin.domain.profile.domain.model.Rating
import com.jdrms.bulletin.domain.profile.domain.model.ReviewId
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ProfileDto
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ReviewDto
import com.jdrms.bulletin.domain.profile.infrastructure.mapper.ProfileMapper
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.ProfilePhotoStorage
import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabaseProfilePhotoRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileInfrastructureTestPart1 {

    private class FakeProfilePhotoStorage(
        private val failure: Throwable? = null,
        private val authenticatedUserId: String? = "student-123"
    ) : ProfilePhotoStorage {
        var path: String? = null
        var mediaType: String? = null

        override fun authenticatedUserId(): String? = authenticatedUserId

        override suspend fun upload(path: String, bytes: ByteArray, mediaType: String) {
            failure?.let { throw it }
            this.path = path
            this.mediaType = mediaType
        }

        override fun publicUrl(path: String): String = "https://cdn.example/pfp/$path"
    }

    private fun jpegBytes(): ByteArray = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0x01)

    private fun pngBytes(): ByteArray = byteArrayOf(
        0x89.toByte(),
        0x50,
        0x4e,
        0x47,
        0x0d,
        0x0a,
        0x1a,
        0x0a,
        0x01
    )

    @Test
    fun supabasePhotoRepositoryUsesOwnedStablePathAndPublicCacheBustedUrl() = runTest {
        val storage = FakeProfilePhotoStorage()
        val photo = (ProfilePhoto.create(pngBytes(), "image/png") as Result.Success).data

        val result = SupabaseProfilePhotoRepository(storage) { "version-1" }
            .upload(UserId("student-123"), photo)

        assertTrue(result is Result.Success)
        assertEquals("student-123/avatar", storage.path)
        assertEquals("image/png", storage.mediaType)
        assertEquals("https://cdn.example/pfp/student-123/avatar?v=version-1", result.data.value)
    }

    @Test
    fun supabasePhotoRepositoryMapsStorageFailures() = runTest {
        val storage = FakeProfilePhotoStorage(IllegalStateException("row-level security denied"))
        val photo = (ProfilePhoto.create(jpegBytes(), "image/jpeg") as Result.Success).data

        val result = SupabaseProfilePhotoRepository(storage).upload(UserId("student-123"), photo)

        assertTrue(result is Result.Error)
        assertEquals(
            "You do not have permission to upload this profile photo.",
            result.exception.message
        )
    }

    @Test
    fun supabasePhotoRepositoryDoesNotUploadToAnotherUsersPath() = runTest {
        val storage = FakeProfilePhotoStorage(authenticatedUserId = "student-456")
        val photo = (ProfilePhoto.create(jpegBytes(), "image/jpeg") as Result.Success).data

        val result = SupabaseProfilePhotoRepository(storage).upload(UserId("student-123"), photo)

        assertTrue(result is Result.Error)
        assertEquals("You do not have permission to upload this profile photo.", result.exception.message)
        assertNull(storage.path)
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
        assertEquals("", profile.university)
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
            university = "CSULB",
            bio = "Campus student"
        )

        val updateDto = ProfileMapper.toUpdateDto(profile)
        assertEquals("John Doe", updateDto.fullName)
        assertEquals("Computer Science", updateDto.major)
        assertEquals("CSULB", updateDto.university)
        assertEquals("Campus student", updateDto.bio)
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
}
