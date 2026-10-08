package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.domain.profile.application.GetProfileActivity
import com.jdrms.bulletin.domain.profile.application.GetProfileOverview
import com.jdrms.bulletin.domain.profile.application.ProfileActiveListingsProvider
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
}
