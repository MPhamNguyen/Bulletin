package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListingFilterOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
            onSignOut = {}
        )
        defaultActions.onDeleteProfile()

        val customActions = SettingsActions(
            onEditAccount = {},
            onViewPublicProfile = {},
            onNotifications = {},
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
            onSignOut = {}
        )
        defaultActions.onConfirmDeleteProfile()

        val customActions = SettingsActions(
            onEditAccount = {},
            onViewPublicProfile = {},
            onNotifications = {},
            onSignOut = {},
            onConfirmDeleteProfile = { confirmed = true }
        )
        customActions.onConfirmDeleteProfile()
        assertTrue(confirmed)
    }

    @Test
    fun formatHoldHintFormatsSecondsCorrectly() {
        assertEquals("Press and hold for 1.5 seconds", formatHoldHint(1500))
        assertEquals("Press and hold for 2 seconds", formatHoldHint(2000))
    }

    @Test
    fun bookmarkedListingsWaitForPaginationBeforeShowingEmptyState() {
        val state = BookmarkedListingsViewState(
            listings = emptyList(),
            allListings = emptyList(),
            filters = ProfileListingFilterState(),
            filterOptions = ProfileBookmarkedListingFilterOptions(),
            isLoading = false,
            isLoadingMore = false,
            hasMore = true,
            errorMessage = null,
            removingListingId = null
        )

        assertFalse(state.shouldShowEmptyState)
        assertTrue(state.copy(hasMore = false).shouldShowEmptyState)
    }
}
