package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingPage
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerProfile
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerSalesProvider
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceSellerProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SellerProfilePreviewTest {
    @Test
    fun knownIdentityIsAvailableImmediatelyAndReplacedByFetchedProfile() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val model = model()
            val preview = SellerProfilePreview("seller", "Known name", "photo")
            model.load("seller", preview)
            assertTrue(model.uiState.value.isLoading)
            assertEquals(preview, model.uiState.value.preview)
            assertNull(model.uiState.value.profile)
            advanceUntilIdle()
            assertEquals("Updated name", model.uiState.value.profile?.name)
            assertNull(model.uiState.value.preview)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun switchingSellersDiscardsPreviousIdentityAndRejectsMismatchedPreview() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val model = model()
            val preview = SellerProfilePreview("first", "First seller", "photo")
            model.load("first", preview)
            model.load("second", preview)
            assertNull(model.uiState.value.preview)
            assertNull(model.uiState.value.profile)
            advanceUntilIdle()
            assertEquals("second", model.uiState.value.profile?.sellerId)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun model() = SellerProfileViewModel(
        ViewMarketplaceSellerProfile(
            profileProvider = { MarketplaceSellerProfile(it, "Updated name", null) },
            listingSource = MarketplaceListingSource { MarketplaceListingPage(emptyList(), null) },
            sellerSalesProvider = MarketplaceSellerSalesProvider { 0 }
        )
    )
}
