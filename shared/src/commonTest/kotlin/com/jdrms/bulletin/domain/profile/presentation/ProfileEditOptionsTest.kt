package com.jdrms.bulletin.domain.profile.presentation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileEditOptionsTest {
    private val schools = listOf(
        ComboOption("University of Southern California", listOf("USC")),
        ComboOption("California State University, Long Beach", listOf("CSULB", "Cal State Long Beach"))
    )

    @Test
    fun canonicalSchoolLabelIsRecognizedWithoutItsAcronym() {
        assertTrue(schools.isKnownOption("University of Southern California"))
        assertFalse(schools.isKnownOption("University of Southern California (USC)"))
    }

    @Test
    fun schoolKeywordsOnlyHelpSearchAndDoNotChangeTheSelectedLabel() {
        val usc = schools.first()

        assertTrue(usc.matches("usc"))
        assertTrue(usc.matches("southern california"))
        assertFalse(usc.matches("ucla"))
        assertTrue(usc.label == "University of Southern California")
    }
}
