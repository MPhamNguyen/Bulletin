package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy
import com.jdrms.bulletin.domain.profile.infrastructure.mapper.ProfileMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StudentProfileDetailsTest {

    private val policy = ProfileValidationPolicy()

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
            bio = "  Campus seller and student.  ",
        )

        assertTrue(result is Result.Success)
        assertEquals("John Doe", result.data.fullName)
        assertEquals("Computer Science", result.data.major)
        assertEquals("California State University - Long Beach", result.data.university)
        assertEquals("Campus seller and student.", result.data.bio)
        assertEquals("Original Name", profile.fullName)
    }

    @Test
    fun testStudentProfileRemovesTrailingAcronymsFromAcademicDetails() {
        val profile = StudentProfile(
            id = UserId("student_1"),
            email = StudentEmail("student@example.com"),
            fullName = "Original Name"
        )

        val result = profile.updateDetails(
            fullName = "John Doe",
            major = "Computer Science (CS)",
            university = "California State University, Long Beach (CSULB)",
            bio = "Student"
        )

        assertTrue(result is Result.Success)
        assertEquals("Computer Science", result.data.major)
        assertEquals("California State University, Long Beach", result.data.university)
    }

    @Test
    fun testStudentProfilePreservesNonAcronymParentheticalAcademicDetails() {
        val profile = StudentProfile(
            id = UserId("student_1"),
            email = StudentEmail("student@example.com"),
            fullName = "Original Name"
        )

        val result = profile.updateDetails(
            fullName = "John Doe",
            major = "Art (Pre-Med)",
            university = "Test University (Downtown)",
            bio = "Student"
        )

        assertTrue(result is Result.Success)
        assertEquals("Art (Pre-Med)", result.data.major)
        assertEquals("Test University (Downtown)", result.data.university)
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
        assertTrue(blankSchool is Result.Success)
        assertEquals("", blankSchool.data.university)

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
    fun testProfileMapperPreservesMajor() {
        val profile = StudentProfile(
            id = UserId("student_1"),
            email = StudentEmail("student@example.com"),
            fullName = "John Doe",
            major = "Computer Science",
        )

        val dto = ProfileMapper.toDto(profile)
        assertEquals("Computer Science", dto.major)
        assertEquals(profile, ProfileMapper.toDomain(dto))
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
