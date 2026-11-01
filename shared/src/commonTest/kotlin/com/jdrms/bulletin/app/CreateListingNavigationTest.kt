package com.jdrms.bulletin.app

import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.app.navigation.MutableMainNavigationState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CreateListingNavigationTest {
    @Test
    fun closingReturnsToThePageTheSellerCameFrom() {
        val navigation = MutableMainNavigationState()
        navigation.currentDestination = AppDestination.MARKETPLACE

        navigation.openCreateListing()
        assertEquals(AppDestination.LISTINGS, navigation.currentDestination)

        navigation.closeCreateListing()
        assertEquals(AppDestination.MARKETPLACE, navigation.currentDestination)
        assertNull(navigation.createListingReturn)
    }

    @Test
    fun reopeningWhileAlreadyOnCreateListingKeepsTheOriginalReturnPage() {
        val navigation = MutableMainNavigationState()
        navigation.currentDestination = AppDestination.MESSAGES

        navigation.openCreateListing()
        navigation.openCreateListing()
        navigation.closeCreateListing()

        assertEquals(AppDestination.MESSAGES, navigation.currentDestination)
    }

    @Test
    fun closingWithoutARecordedPageFallsBackToHome() {
        val navigation = MutableMainNavigationState()
        navigation.currentDestination = AppDestination.LISTINGS

        navigation.closeCreateListing()

        assertEquals(AppDestination.HOME, navigation.currentDestination)
    }
}
