package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.service.ProfileValidationPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StudentEmailPolicyTest {

    private val policy = ProfileValidationPolicy()

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
    fun testNonEduEmailFailsUniversityValidation() {
        val result = policy.validateUniversityRegistration("user@gmail.com")
        assertTrue(result.isError())
    }

    @Test
    fun testEduEmailPassesUniversityValidation() {
        val result = policy.validateUniversityRegistration("student@csulb.edu")
        assertTrue(result.isSuccess())
    }
}
