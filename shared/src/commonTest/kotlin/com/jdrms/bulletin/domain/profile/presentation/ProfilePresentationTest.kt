package com.jdrms.bulletin.domain.profile.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun settingsActionsDefaultsDeleteProfileToNoOp() {
        var clicked = false
        val defaultActions = SettingsActions(
            onEditAccount = {},
            onViewPublicProfile = {},
            onNotifications = {},
            onPrivacy = {},
            onHelpSupport = {},
            onTermsConditions = {},
            onSignOut = {}
        )
        defaultActions.onDeleteProfile()

        val customActions = SettingsActions(
            onEditAccount = {},
            onViewPublicProfile = {},
            onNotifications = {},
            onPrivacy = {},
            onHelpSupport = {},
            onTermsConditions = {},
            onSignOut = {},
            onDeleteProfile = { clicked = true }
        )
        customActions.onDeleteProfile()
        assertTrue(clicked)
    }

    @Test
    fun settingsActionsDefaultsConfirmDeleteProfileToNoOp() {
        var confirmed = false
        val defaultActions = SettingsActions(
            onEditAccount = {},
            onViewPublicProfile = {},
            onNotifications = {},
            onPrivacy = {},
            onHelpSupport = {},
            onTermsConditions = {},
            onSignOut = {}
        )
        defaultActions.onConfirmDeleteProfile()

        val customActions = SettingsActions(
            onEditAccount = {},
            onViewPublicProfile = {},
            onNotifications = {},
            onPrivacy = {},
            onHelpSupport = {},
            onTermsConditions = {},
            onSignOut = {},
            onConfirmDeleteProfile = { confirmed = true }
        )
        customActions.onConfirmDeleteProfile()
        assertTrue(confirmed)
    }

    @Test
    fun formatListingsConsequenceDescribesSoftDeleteWithoutClaimingSideEffects() {
        assertEquals("Your profile is marked for soft-delete", formatListingsConsequence(3))
        assertEquals("Your profile is marked for soft-delete", formatListingsConsequence(0))
    }

    @Test
    fun formatHoldHintFormatsSecondsCorrectly() {
        assertEquals("Press and hold for 1.5 seconds", formatHoldHint(1500))
        assertEquals("Press and hold for 2 seconds", formatHoldHint(2000))
    }
}
