package com.jdrms.bulletin.app

import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.app.navigation.MutableMainNavigationState
import kotlin.test.Test
import kotlin.test.assertEquals

class MarketplaceTabReselectionTest {
    @Test
    fun onlyReselectingMarketplaceRequestsScrollToTop() {
        val navigation = MutableMainNavigationState()
        navigation.selectTab(AppDestination.MARKETPLACE)
        assertEquals(0, navigation.marketplaceScrollToTopRequest)
        navigation.selectTab(AppDestination.MARKETPLACE)
        assertEquals(1, navigation.marketplaceScrollToTopRequest)
        navigation.selectTab(AppDestination.MARKETPLACE)
        assertEquals(2, navigation.marketplaceScrollToTopRequest)

        AppDestination.entries.filter { it != AppDestination.MARKETPLACE }.forEach {
            navigation.selectTab(it)
            navigation.selectTab(it)
            navigation.selectTab(AppDestination.MARKETPLACE)
            assertEquals(2, navigation.marketplaceScrollToTopRequest)
        }
    }
}
