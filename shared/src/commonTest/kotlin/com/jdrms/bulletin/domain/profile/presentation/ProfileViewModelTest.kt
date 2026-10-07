package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.domain.profile.application.GetProfileActivity
import com.jdrms.bulletin.domain.profile.application.GetProfileOverview
import com.jdrms.bulletin.domain.profile.application.ProfileActiveListingsProvider
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListing
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListingRemover
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListingsPage
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListingsProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    @Test
    fun profileReadModelTracksSessionAndListingCount() = profileTest {
        val fixture = ProfileFixture()
        val profile = fixture.authenticate()
        var listings = 2
        val listingsProvider = ProfileActiveListingsProvider { listings }
        val vm = ProfileViewModel(
            fixture.session,
            GetProfileOverview(fixture.profiles, listingsProvider),
            GetProfileActivity(listingsProvider),
            RefreshSignal()
        )
        advanceUntilIdle()
        assertEquals(profile.id, vm.uiState.value.profile?.id)
        assertEquals(2, vm.uiState.value.activeListingsCount)
        assertNull(vm.uiState.value.itemsSoldCount)
        listings = 3
        vm.refreshActiveListings()
        advanceUntilIdle()
        assertEquals(3, vm.uiState.value.activeListingsCount)
        fixture.session.unauthenticated()
        advanceUntilIdle()
        assertNull(vm.uiState.value.profile)
    }

    @Test
    fun bookmarkedListingsLoadAndCanBeRemoved() = profileTest {
        val fixture = ProfileFixture()
        fixture.authenticate()
        val listing = ProfileBookmarkedListing(
            id = "listing-1",
            title = "Desk Lamp",
            sellerName = "Alex",
            price = "${'$'}15.00",
            category = "OTHER"
        )
        var bookmarks = listOf(listing)
        val bookmarksProvider = ProfileBookmarkedListingsProvider { _, _, _ ->
            ProfileBookmarkedListingsPage(bookmarks, null)
        }
        val bookmarkRemover = ProfileBookmarkedListingRemover { _, listingId ->
            bookmarks = bookmarks.filterNot { it.id == listingId }
        }
        val listingsProvider = ProfileActiveListingsProvider { 0 }
        val vm = ProfileViewModel(
            sessionRepository = fixture.session,
            getProfileOverview = GetProfileOverview(fixture.profiles, listingsProvider),
            getProfileActivity = GetProfileActivity(listingsProvider),
            listingChangedSignal = RefreshSignal(),
            bookmarkedListingsProvider = bookmarksProvider,
            bookmarkedListingRemover = bookmarkRemover
        )
        advanceUntilIdle()

        vm.loadBookmarkedListings()
        advanceUntilIdle()
        assertEquals(listOf(listing), vm.uiState.value.bookmarkedListings)

        vm.removeBookmarkedListing(listing.id)
        advanceUntilIdle()
        assertEquals(emptyList(), vm.uiState.value.bookmarkedListings)
    }
}
