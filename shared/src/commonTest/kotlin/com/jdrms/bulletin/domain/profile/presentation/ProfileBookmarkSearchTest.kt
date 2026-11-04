package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.domain.profile.application.GetProfileActivity
import com.jdrms.bulletin.domain.profile.application.GetProfileOverview
import com.jdrms.bulletin.domain.profile.application.ProfileActiveListingsProvider
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListing
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListingsPage
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListingsProvider
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarksPageCursor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileBookmarkSearchTest {
    @Test
    fun queryKeywordsAreComputedWhenTheQueryChanges() {
        val listing = bookmark("book", "Physics Book", "TEXTBOOKS")
        val state = ProfileUiState(
            bookmarkedListings = listOf(listing),
            bookmarkedListingFilters = ProfileListingFilterState().withQuery("  physics   textbooks ")
        )

        assertEquals(listOf("physics", "textbooks"), state.bookmarkedListingFilters.queryKeywords)
        assertEquals(listOf(listing), state.filteredBookmarkedListings)
    }

    @Test
    fun rapidQueryChangesLoadRemainingPagesOnceAfterTheDebounce() = profileTest {
        val fixture = ProfileFixture()
        fixture.authenticate()
        val requestedOffsets = mutableListOf<Int?>()
        val provider = ProfileBookmarkedListingsProvider { _, cursor, _ ->
            requestedOffsets += cursor?.offset
            when (cursor?.offset) {
                null -> ProfileBookmarkedListingsPage(
                    listOf(bookmark("lamp", "Lamp", "OTHER")),
                    ProfileBookmarksPageCursor(1)
                )
                1 -> ProfileBookmarkedListingsPage(listOf(bookmark("book", "Physics Book", "TEXTBOOKS")), null)
                else -> error("Unexpected bookmark cursor: ${cursor.offset}")
            }
        }
        val listings = ProfileActiveListingsProvider { 0 }
        val viewModel = ProfileViewModel(
            sessionRepository = fixture.session,
            getProfileOverview = GetProfileOverview(fixture.profiles, listings),
            getProfileActivity = GetProfileActivity(listings),
            listingChangedSignal = RefreshSignal(),
            bookmarks = ProfileBookmarksDependencies(provider)
        )
        advanceUntilIdle()
        viewModel.loadBookmarkedListings()
        advanceUntilIdle()

        viewModel.onBookmarkedQueryChanged("p")
        viewModel.onBookmarkedQueryChanged("ph")
        viewModel.onBookmarkedQueryChanged("physics")
        runCurrent()
        advanceTimeBy(299)
        runCurrent()
        assertEquals(listOf<Int?>(null), requestedOffsets)

        advanceTimeBy(1)
        advanceUntilIdle()
        assertEquals(listOf<Int?>(null, 1), requestedOffsets)
        assertEquals(listOf("book"), viewModel.uiState.value.filteredBookmarkedListings.map { it.id })
    }

    private fun bookmark(id: String, title: String, category: String) = ProfileBookmarkedListing(
        id = id,
        title = title,
        sellerName = "Seller",
        price = "${'$'}10.00",
        category = category
    )
}
