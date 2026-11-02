package com.jdrms.bulletin.domain.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.GetProfileActivity
import com.jdrms.bulletin.domain.profile.application.GetProfileOverview
import com.jdrms.bulletin.domain.profile.application.ProfileSoldListingsProvider
import com.jdrms.bulletin.domain.profile.application.SessionRepository
import com.jdrms.bulletin.domain.profile.application.SessionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Read model for the profile landing destination and its bookmarked-listings child destination. */
class ProfileViewModel(
    private val sessionRepository: SessionRepository,
    private val getProfileOverview: GetProfileOverview,
    private val getProfileActivity: GetProfileActivity,
    listingChangedSignal: RefreshSignal,
    private val soldListingsProvider: ProfileSoldListingsProvider? = null,
    private val bookmarks: ProfileBookmarksDependencies = ProfileBookmarksDependencies()
) : ViewModel() {
    private val state = MutableStateFlow(ProfileUiState())
    val uiState = state.asStateFlow()
    private val bookmarksController = ProfileBookmarksController(state, viewModelScope, bookmarks)

    init {
        viewModelScope.launch {
            sessionRepository.state.collect { session ->
                when (session) {
                    is SessionState.Authenticated -> {
                        state.update { it.copy(profile = session.profile) }
                        refreshProfileDetails()
                    }
                    SessionState.Checking -> Unit
                    SessionState.Unauthenticated -> state.value = ProfileUiState()
                }
            }
        }
        viewModelScope.launch { listingChangedSignal.events.collect { refreshActiveListings() } }
    }

    fun refreshActiveListings() {
        val profile = state.value.profile ?: return
        viewModelScope.launch {
            val activeCount = getProfileActivity(profile.id)
            val soldCount = soldListingsProvider?.getSoldListingsCount(profile.id)
            state.update {
                it.copy(
                    activeListingsCount = activeCount,
                    itemsSoldCount = soldCount
                )
            }
        }
    }

    fun refreshProfile() {
        if (state.value.isRefreshing) return
        viewModelScope.launch {
            state.update { it.copy(isRefreshing = true, profileRefreshComplete = false, errorMessage = null) }
            try {
                if (refreshProfileDetails(forceRefresh = true)) {
                    state.update { it.copy(profileRefreshComplete = true) }
                    delay(PROFILE_REFRESH_COMPLETE_MILLIS)
                }
            } finally {
                state.update { it.copy(isRefreshing = false, profileRefreshComplete = false) }
            }
        }
    }

    fun loadBookmarkedListings() {
        bookmarksController.load()
    }

    fun loadMoreBookmarkedListings() {
        bookmarksController.loadMore()
    }

    fun viewBookmarkedListing(listingId: String) {
        if (state.value.bookmarkedListings.none { it.id == listingId }) return
        state.update { it.copy(selectedBookmarkedListingId = listingId, errorMessage = null) }
    }

    fun dismissBookmarkedListing() {
        state.update { it.copy(selectedBookmarkedListingId = null, errorMessage = null) }
    }

    fun onBookmarkedQueryChanged(query: String) {
        bookmarksController.updateQuery(query)
    }

    fun onBookmarkedFiltersApplied(category: String?, condition: String?) {
        bookmarksController.applyFilters(category, condition)
    }

    fun removeBookmarkedListing(listingId: String) {
        bookmarksController.remove(listingId)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun refreshProfileDetails(forceRefresh: Boolean = false): Boolean {
        val profile = state.value.profile ?: return false
        return try {
            val result = if (forceRefresh) {
                getProfileOverview.refresh(profile.id)
            } else {
                getProfileOverview(profile.id)
            }
            when (result) {
                is Result.Success -> {
                    result.data?.let { overview ->
                        val soldCount = soldListingsProvider?.getSoldListingsCount(profile.id)
                        state.update {
                            it.copy(
                                profile = overview.profile,
                                reputation = overview.reputation,
                                activeListingsCount = overview.activeListingsCount,
                                itemsSoldCount = soldCount,
                                hasLoadedOverview = true
                            )
                        }
                        true
                    } ?: false
                }
                is Result.Error -> {
                    state.update {
                        it.copy(errorMessage = result.exception.message ?: "Failed to load profile")
                    }
                    false
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            state.update { it.copy(errorMessage = error.message ?: "Failed to load profile") }
            false
        }
    }

    private companion object {
        const val PROFILE_REFRESH_COMPLETE_MILLIS = 650L
    }
}
