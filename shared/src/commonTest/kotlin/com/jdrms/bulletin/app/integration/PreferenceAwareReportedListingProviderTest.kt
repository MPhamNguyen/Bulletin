package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceReportedListingProvider
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId
import com.jdrms.bulletin.domain.marketplace.infrastructure.InMemoryReportedListingVisibilityPreferenceStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PreferenceAwareReportedListingProviderTest {
    @Test
    fun returnsReportedListingsByDefaultAndWhenHidingIsEnabled() = runTest {
        val reporterId = MarketplaceReporterId("reporter")
        val reportedIds = setOf(MarketplaceItemId("listing:one"))
        val preferenceStore = InMemoryReportedListingVisibilityPreferenceStore()
        val provider = PreferenceAwareReportedListingProvider(
            MarketplaceReportedListingProvider { reportedIds },
            preferenceStore
        )

        assertEquals(reportedIds, provider.getReportedListingIds(reporterId))

        preferenceStore.setHideReportedListings(reporterId, true)
        assertEquals(reportedIds, provider.getReportedListingIds(reporterId))
    }

    @Test
    fun returnsNoReportedListingsWhenReporterChoosesToKeepThemVisible() = runTest {
        val reporterId = MarketplaceReporterId("reporter")
        val reportedIds = setOf(MarketplaceItemId("listing:one"))
        val preferenceStore = InMemoryReportedListingVisibilityPreferenceStore()
        preferenceStore.setHideReportedListings(reporterId, false)
        val provider = PreferenceAwareReportedListingProvider(
            MarketplaceReportedListingProvider { reportedIds },
            preferenceStore
        )

        assertEquals(emptySet(), provider.getReportedListingIds(reporterId))
    }

    @Test
    fun preferenceIsIsolatedPerReporter() = runTest {
        val preferenceStore = InMemoryReportedListingVisibilityPreferenceStore()
        preferenceStore.setHideReportedListings(MarketplaceReporterId("reporter-one"), false)

        assertEquals(true, preferenceStore.shouldHideReportedListings(MarketplaceReporterId("reporter-two")))
    }
}
