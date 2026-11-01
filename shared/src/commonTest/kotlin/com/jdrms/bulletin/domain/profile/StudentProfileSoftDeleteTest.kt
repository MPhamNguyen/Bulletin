package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StudentProfileSoftDeleteTest {

    @Test
    fun testSoftDeleteSetsTimestampAndIsDeleted() {
        val profile = StudentProfile(
            id = UserId("student_1"),
            email = StudentEmail("student@example.com"),
            fullName = "John Doe"
        )
        assertFalse(profile.isDeleted)
        assertNull(profile.deletedAt)

        val timestamp = "2026-10-06T19:00:00Z"
        val result = profile.softDelete(timestamp)

        assertTrue(result is Result.Success)
        val deleted = result.data
        assertTrue(deleted.isDeleted)
        assertEquals(timestamp, deleted.deletedAt)
    }

    @Test
    fun testSoftDeleteRejectsAlreadyDeletedOrBlankTimestamp() {
        val profile = StudentProfile(
            id = UserId("student_1"),
            email = StudentEmail("student@example.com"),
            fullName = "John Doe",
            deletedAt = "2026-10-06T19:00:00Z"
        )
        assertTrue(profile.isDeleted)

        val reDeleteResult = profile.softDelete("2026-10-06T19:05:00Z")
        assertTrue(reDeleteResult is Result.Error)
        assertEquals("Profile is already deleted.", reDeleteResult.exception.message)

        val activeProfile = StudentProfile(
            id = UserId("student_2"),
            email = StudentEmail("student2@example.com"),
            fullName = "Jane Doe"
        )
        val blankResult = activeProfile.softDelete("   ")
        assertTrue(blankResult is Result.Error)
        assertEquals("Deletion timestamp cannot be blank.", blankResult.exception.message)
    }

    @Test
    fun testUpdateDetailsRejectsSoftDeletedProfile() {
        val profile = StudentProfile(
            id = UserId("student_1"),
            email = StudentEmail("student@example.com"),
            fullName = "John Doe",
            deletedAt = "2026-10-06T19:00:00Z"
        )

        val updateResult = profile.updateDetails(
            fullName = "Updated Name",
            major = "Biology",
            university = "CSULB",
            bio = "New bio"
        )

        assertTrue(updateResult is Result.Error)
        assertEquals("Cannot update a deleted profile.", updateResult.exception.message)
    }
}
