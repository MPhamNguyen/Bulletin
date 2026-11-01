package com.jdrms.bulletin.domain.marketplace.infrastructure

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidReportedListingVisibilityPreferenceStoreTest {
    @Test
    fun defaultsToHidingReportedListings() = runTest {
        val store = AndroidReportedListingVisibilityPreferenceStore(InMemoryPreferencesDataStore())

        assertEquals(true, store.shouldHideReportedListings(reporterId("reporter")))
    }

    @Test
    fun savedPreferenceSurvivesStoreRecreationAndIsScopedToReporter() = runTest {
        val dataStore = InMemoryPreferencesDataStore()
        AndroidReportedListingVisibilityPreferenceStore(dataStore)
            .setHideReportedListings(reporterId("reporter-one"), false)
        val recreatedStore = AndroidReportedListingVisibilityPreferenceStore(dataStore)

        assertEquals(false, recreatedStore.shouldHideReportedListings(reporterId("reporter-one")))
        assertEquals(true, recreatedStore.shouldHideReportedListings(reporterId("reporter-two")))
    }

    private fun reporterId(value: String) =
        MarketplaceReporterId(value)
}
