package com.jdrms.bulletin.domain.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.GetProfileActivity
import com.jdrms.bulletin.domain.profile.application.GetProfileOverview
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListing
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListingRemover
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListingsPage
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListingsProvider
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarksPageCursor
import com.jdrms.bulletin.domain.profile.application.SessionRepository
import com.jdrms.bulletin.domain.profile.application.SessionState
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.StudentReputation
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: StudentProfile? = null,
    val reputation: StudentReputation? = null,
    val activeListingsCount: Int = 0,
    val itemsSoldCount: Int? = null,
    val bookmarkedListings: List<ProfileBookmarkedListing> = emptyList(),
    val isLoadingBookmarkedListings: Boolean = false,
    val isLoadingMoreBookmarkedListings: Boolean = false,
    val bookmarkedListingsNextCursor: ProfileBookmarksPageCursor? = null,
    val selectedBookmarkedListingId: String? = null,
    val removingBookmarkedListingId: String? = null,
    val errorMessage: String? = null
) {
    val selectedBookmarkedListing: ProfileBookmarkedListing?
        get() = bookmarkedListings.firstOrNull { it.id == selectedBookmarkedListingId }
}

data class ProfileBookmarksDependencies(
    val provider: ProfileBookmarkedListingsProvider? = null,
    val remover: ProfileBookmarkedListingRemover? = null
)

/** Read model for the profile landing destination and its bookmarked-listings child destination. */
class ProfileViewModel(
    private val sessionRepository: SessionRepository,
    private val getProfileOverview: GetProfileOverview,
    private val getProfileActivity: GetProfileActivity,
    listingChangedSignal: RefreshSignal,
    private val bookmarks: ProfileBookmarksDependencies = ProfileBookmarksDependencies()
) : ViewModel() {
    private val state = MutableStateFlow(ProfileUiState())
    val uiState = state.asStateFlow()
    private var bookmarkedListingsJob: Job? = null
    private var bookmarkRemovalJob: Job? = null

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
            val count = getProfileActivity(profile.id)
            state.update { it.copy(activeListingsCount = count) }
        }
    }

    fun loadBookmarkedListings() {
        bookmarkedListingsJob?.cancel()
        bookmarkedListingsJob = viewModelScope.launch {
            state.update {
                it.copy(
                    isLoadingBookmarkedListings = true,
                    isLoadingMoreBookmarkedListings = false,
                    errorMessage = null
                )
            }
            val userId = state.value.profile?.id
            if (userId == null) {
                state.update {
                    it.copy(
                        bookmarkedListings = emptyList(),
                        isLoadingBookmarkedListings = false,
                        bookmarkedListingsNextCursor = null,
                        errorMessage = "Sign in to view bookmarked listings."
                    )
                }
                return@launch
            }
            loadInitialBookmarks(userId)
        }
    }

    private suspend fun loadInitialBookmarks(userId: UserId) {
        try {
            val page = getBookmarkedListingsPage(userId, null)
            state.update {
                it.copy(
                    bookmarkedListings = page.listings,
                    isLoadingBookmarkedListings = false,
                    bookmarkedListingsNextCursor = page.nextCursor,
                    selectedBookmarkedListingId = null,
                    errorMessage = null
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            state.update {
                it.copy(
                    isLoadingBookmarkedListings = false,
                    bookmarkedListingsNextCursor = null,
                    errorMessage = "Unable to load bookmarked listings. Please try again."
                )
            }
        }
    }

    fun loadMoreBookmarkedListings() {
        val current = state.value
        val cursor = current.bookmarkedListingsNextCursor ?: return
        val userId = current.profile?.id ?: return
        if (current.isLoadingBookmarkedListings || current.isLoadingMoreBookmarkedListings) return
        bookmarkedListingsJob = viewModelScope.launch {
            state.update { it.copy(isLoadingMoreBookmarkedListings = true, errorMessage = null) }
            try {
                val page = getBookmarkedListingsPage(userId, cursor)
                state.update {
                    it.copy(
                        bookmarkedListings = (it.bookmarkedListings + page.listings).distinctBy { listing ->
                            listing.id
                        },
                        isLoadingMoreBookmarkedListings = false,
                        bookmarkedListingsNextCursor = page.nextCursor
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state.update {
                    it.copy(
                        isLoadingMoreBookmarkedListings = false,
                        errorMessage = "Unable to load more bookmarked listings. Please try again."
                    )
                }
            }
        }
    }

    fun viewBookmarkedListing(listingId: String) {
        if (state.value.bookmarkedListings.none { it.id == listingId }) return
        state.update { it.copy(selectedBookmarkedListingId = listingId, errorMessage = null) }
    }

    fun dismissBookmarkedListing() {
        state.update { it.copy(selectedBookmarkedListingId = null, errorMessage = null) }
    }

    fun removeBookmarkedListing(listingId: String) {
        val current = state.value
        val userId = current.profile?.id ?: return
        val removalUnavailable = current.removingBookmarkedListingId != null ||
            current.bookmarkedListings.none { it.id == listingId }
        if (removalUnavailable) return
        bookmarkRemovalJob?.cancel()
        bookmarkedListingsJob?.cancel()
        bookmarkRemovalJob = viewModelScope.launch {
            state.update { it.copy(removingBookmarkedListingId = listingId, errorMessage = null) }
            performBookmarkRemoval(userId, listingId)
        }
    }

    private suspend fun performBookmarkRemoval(userId: UserId, listingId: String) {
        var removed = false
        try {
            val remover = bookmarks.remover ?: error("Bookmark removal is unavailable.")
            remover.removeBookmark(userId, listingId)
            removed = true
            state.update {
                it.copy(
                    bookmarkedListings = it.bookmarkedListings.filterNot { listing -> listing.id == listingId },
                    selectedBookmarkedListingId = null
                )
            }
            val page = getBookmarkedListingsPage(userId, null)
            state.update {
                it.copy(
                    bookmarkedListings = page.listings,
                    bookmarkedListingsNextCursor = page.nextCursor,
                    removingBookmarkedListingId = null,
                    errorMessage = null
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            val message = if (removed) {
                "Bookmark removed, but the list could not be refreshed."
            } else {
                "Unable to remove bookmark. Please try again."
            }
            state.update { it.copy(removingBookmarkedListingId = null, errorMessage = message) }
        }
    }

    private suspend fun getBookmarkedListingsPage(
        userId: UserId,
        cursor: ProfileBookmarksPageCursor?
    ): ProfileBookmarkedListingsPage = bookmarks.provider?.getBookmarkedListings(
        userId,
        cursor,
        BOOKMARKS_PAGE_SIZE
    ) ?: ProfileBookmarkedListingsPage(emptyList(), null)

    private suspend fun refreshProfileDetails() {
        val profile = state.value.profile ?: return
        when (val result = getProfileOverview(profile.id)) {
            is Result.Success -> result.data?.let { overview ->
                state.update {
                    it.copy(
                        profile = overview.profile,
                        reputation = overview.reputation,
                        activeListingsCount = overview.activeListingsCount
                    )
                }
            }
            is Result.Error -> state.update {
                it.copy(errorMessage = result.exception.message ?: "Failed to load profile")
            }
        }
    }

    private companion object {
        const val BOOKMARKS_PAGE_SIZE = 20
    }
}
