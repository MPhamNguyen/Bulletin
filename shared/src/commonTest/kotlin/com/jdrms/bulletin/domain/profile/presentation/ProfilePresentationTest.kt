package com.jdrms.bulletin.domain.profile.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class ProfilePresentationTest {

    @Test
    fun profileInitialsUseFirstAndLastNameParts() {
        assertEquals("AM", profileInitials("Alex Morgan"))
        assertEquals("AM", profileInitials("  Alex   Lee Morgan  "))
    }

    @Test
    fun profileInitialsHandleSingleAndMissingNames() {
        assertEquals("A", profileInitials("alex"))
        assertEquals("ST", profileInitials("   "))
    }
}
