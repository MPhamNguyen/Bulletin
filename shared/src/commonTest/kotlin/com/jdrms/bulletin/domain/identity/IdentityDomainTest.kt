package com.jdrms.bulletin.domain.identity

import com.jdrms.bulletin.domain.identity.domain.model.Email
import com.jdrms.bulletin.domain.identity.domain.service.IdentityValidationPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class IdentityDomainTest {

    private val policy = IdentityValidationPolicy()

    @Test
    fun testValidUniversityEmail() {
        val email = Email("student@csulb.edu")
        assertTrue(email.isUniversityEmail)
        assertEquals("student@csulb.edu", email.value)
    }

    @Test
    fun testInvalidEmailThrowsException() {
        assertFailsWith<IllegalArgumentException> {
            Email("invalid-email-string")
        }
    }

    @Test
    fun testNonEduEmailFailsValidation() {
        val result = policy.validateUniversityRegistration("user@gmail.com")
        assertTrue(result.isError())
    }

    @Test
    fun testEduEmailPassesValidation() {
        val result = policy.validateUniversityRegistration("student@csulb.edu")
        assertTrue(result.isSuccess())
    }
}
