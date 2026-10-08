package com.jdrms.bulletin.domain.profile.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileDraftValidationTest {
    @Test
    fun blankSchoolAndMajorAreRejected() {
        val errors = validateProfileDraft(ProfileDraft())

        assertEquals("Enter or select your school.", errors.school)
        assertEquals("Enter or select your major.", errors.major)
        assertTrue(!errors.isValid)
    }

    @Test
    fun customSchoolAndMajorAreValidWhenTheyContainText() {
        val errors = validateProfileDraft(
            ProfileDraft(
                university = "My University",
                major = "My Department",
                universityIsCustom = true,
                majorIsCustom = true
            )
        )

        assertTrue(errors.isValid)
    }
}
