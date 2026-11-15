package com.jdrms.bulletin.app

import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.app.navigation.MutableMainNavigationState
import com.jdrms.bulletin.app.navigation.ProfileDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ListingNavigationTest {
    @Test
    fun closingListingReturnsToOwnPublicProfile() {
        val navigation = MutableMainNavigationState().apply {
            currentDestination = AppDestination.PROFILE
            profileDestination = ProfileDestination.PUBLIC_PROFILE
        }

        navigation.openListing("listing:owned")

        assertEquals(AppDestination.MARKETPLACE, navigation.currentDestination)
        assertEquals("listing:owned", navigation.pendingListingId)

        navigation.closeListing()

        assertEquals(AppDestination.PROFILE, navigation.currentDestination)
        assertEquals(ProfileDestination.PUBLIC_PROFILE, navigation.profileDestination)
        assertNull(navigation.pendingListingId)
        assertNull(navigation.listingReturnSnapshot)
    }
}
