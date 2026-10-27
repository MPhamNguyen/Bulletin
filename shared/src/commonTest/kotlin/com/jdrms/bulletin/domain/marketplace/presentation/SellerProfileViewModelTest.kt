package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingPage
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSnapshot
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageCursor
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerProfile
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerSalesProvider
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceSellerProfile
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class ViewMarketplaceSellerProfileTest {
    @Test
    fun queryRequestsOnlyTheSellersListingsAcrossAllPages() = runTest {
        val cursor = MarketplacePageCursor(Instant.fromEpochMilliseconds(10L), "cursor")
        val requestedSellerIds = mutableListOf<String?>()
        val source = MarketplaceListingSource { request ->
            requestedSellerIds += request.sellerId
            if (request.cursor == null) {
                MarketplaceListingPage(
                    listOf(listing("first", "seller-a")),
                    cursor
                )
            } else {
                MarketplaceListingPage(listOf(listing("second", "seller-a")), null)
            }
        }
        val query = ViewMarketplaceSellerProfile({ sellerProfile(it) }, source, MarketplaceSellerSalesProvider { 3 })

        val details = query("seller-a")

        assertEquals(listOf("first", "second"), details?.listings?.map { it.id.value })
        assertEquals(3, details?.soldListingsCount)
        assertEquals(listOf<String?>("seller-a", "seller-a"), requestedSellerIds)
    }

    @Test
    fun missingProfileDoesNotLoadListings() = runTest {
        var listingLoadCount = 0
        val source = MarketplaceListingSource {
            listingLoadCount += 1
            MarketplaceListingPage(emptyList(), null)
        }

        val details = ViewMarketplaceSellerProfile({ null }, source, MarketplaceSellerSalesProvider { 0 })("missing")

        assertNull(details)
        assertEquals(0, listingLoadCount)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SellerProfileViewModelTest {
    @Test
    fun loadExposesProfileAndSellerListings() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val query = ViewMarketplaceSellerProfile(
                profileProvider = { sellerProfile(it) },
                listingSource = MarketplaceListingSource {
                    MarketplaceListingPage(listOf(listing("available", "seller-a")), null)
                },
                sellerSalesProvider = MarketplaceSellerSalesProvider { 4 }
            )
            val viewModel = SellerProfileViewModel(query)

            viewModel.load("seller-a")
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals("Seller A", viewModel.uiState.value.profile?.name)
            assertEquals(listOf("available"), viewModel.uiState.value.listings.map { it.id.value })
            assertEquals(4, viewModel.uiState.value.soldListingsCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun loadFailureExposesRetryableErrorWithoutProfileData() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val query = ViewMarketplaceSellerProfile(
                profileProvider = { sellerProfile(it) },
                listingSource = MarketplaceListingSource { error("offline") },
                sellerSalesProvider = MarketplaceSellerSalesProvider { 0 }
            )
            val viewModel = SellerProfileViewModel(query)

            viewModel.load("seller-a")
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.profile)
            assertEquals(
                "Unable to load the seller profile. Please try again.",
                viewModel.uiState.value.errorMessage
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun filtersSellerListingsByCategoryAndCondition() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = filteringSellerProfileViewModel()
            viewModel.load("seller-a")
            advanceUntilIdle()

            viewModel.onCategorySelected(MarketplaceCategory.TEXTBOOKS)
            viewModel.onConditionSelected(MarketplaceCondition.NEW)

            assertEquals(listOf("new-book"), viewModel.uiState.value.filteredListings.map { it.id.value })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun openingAnotherSellerCancelsThePreviousRequest() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val firstRequestCancelled = CompletableDeferred<Unit>()
            val query = ViewMarketplaceSellerProfile(
                profileProvider = { sellerId ->
                    if (sellerId == "seller-a") {
                        try {
                            awaitCancellation()
                        } finally {
                            firstRequestCancelled.complete(Unit)
                        }
                    }
                    sellerProfile(sellerId)
                },
                listingSource = MarketplaceListingSource { request ->
                    MarketplaceListingPage(listOf(listing("available", request.sellerId.orEmpty())), null)
                },
                sellerSalesProvider = MarketplaceSellerSalesProvider { 1 }
            )
            val viewModel = SellerProfileViewModel(query)

            viewModel.load("seller-a")
            runCurrent()
            viewModel.load("seller-b")
            advanceUntilIdle()

            assertTrue(firstRequestCancelled.isCompleted)
            assertEquals("seller-b", viewModel.uiState.value.profile?.sellerId)
            assertEquals("seller-b", viewModel.uiState.value.listings.single().sellerId)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun messagingRequiresAuthenticatedNonSellerAndMatchingListingOwner() {
        assertTrue(canMessageMarketplaceSeller("buyer", "seller", "seller"))
        assertFalse(canMessageMarketplaceSeller("seller", "seller", "seller"))
        assertFalse(canMessageMarketplaceSeller(null, "seller", "seller"))
        assertFalse(canMessageMarketplaceSeller("buyer", "seller", "someone-else"))
    }

    @Test
    fun dismissingSellerListingPreservesProfileMessageAction() {
        val catalogListing = SellerListingContext("catalog-item", "seller-a", "Seller A")
        val profileListing = SellerListingContext("profile-item", "seller-a", "Seller A")
        val profile = MarketplaceSellerNavigationState()
            .navigate(MarketplaceDestination.SELLER_PROFILE, catalogListing)
        val detail = profile.navigate(MarketplaceDestination.SELLER_LISTING, profileListing)

        val returnedProfile = detail.navigate(MarketplaceDestination.SELLER_PROFILE)

        assertEquals(MarketplaceDestination.SELLER_PROFILE, returnedProfile.destination)
        assertEquals(profileListing, returnedProfile.listingContext)
        assertNotNull(
            sellerProfileMessageActions(
                currentUserId = "buyer",
                profile = sellerProfile("seller-a"),
                listing = returnedProfile.listingContext,
                actions = MarketplaceScreenActions()
            )
        )
    }
}

private fun filteringSellerProfileViewModel(): SellerProfileViewModel {
    val listings = listOf(
        listing("new-book", "seller-a", MarketplaceCategory.TEXTBOOKS, MarketplaceCondition.NEW),
        listing("used-book", "seller-a", MarketplaceCategory.TEXTBOOKS, MarketplaceCondition.GOOD),
        listing("new-chair", "seller-a", MarketplaceCategory.FURNITURE, MarketplaceCondition.NEW)
    )
    val query = ViewMarketplaceSellerProfile(
        profileProvider = { sellerProfile(it) },
        listingSource = MarketplaceListingSource { MarketplaceListingPage(listings, null) },
        sellerSalesProvider = MarketplaceSellerSalesProvider { 0 }
    )
    return SellerProfileViewModel(query)
}

private fun sellerProfile(sellerId: String) = MarketplaceSellerProfile(
    sellerId = sellerId,
    name = if (sellerId == "seller-a") "Seller A" else "Seller",
    school = "Campus"
)

private fun listing(
    id: String,
    sellerId: String,
    category: MarketplaceCategory = MarketplaceCategory.OTHER,
    condition: MarketplaceCondition = MarketplaceCondition.UNKNOWN
) = MarketplaceListingSnapshot(
    id = id,
    sellerId = sellerId,
    sellerName = "Seller",
    title = id,
    description = "Description",
    priceAmount = 10.0,
    priceCurrency = "USD",
    category = category,
    createdAtMillis = 1L,
    condition = condition
)
