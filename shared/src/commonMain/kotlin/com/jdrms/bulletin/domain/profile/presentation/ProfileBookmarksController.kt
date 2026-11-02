package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListingsPage
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarksPageCursor
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ProfileBookmarksController(
    private val state: MutableStateFlow<ProfileUiState>,
    private val scope: CoroutineScope,
    private val dependencies: ProfileBookmarksDependencies
) {
    private var loadingJob: Job? = null
    private var removalJob: Job? = null

    fun load() {
        loadingJob?.cancel()
        loadingJob = scope.launch {
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
            loadInitialPage(userId)
        }
    }

    fun loadMore() {
        val current = state.value
        val cursor = current.bookmarkedListingsNextCursor ?: return
        val userId = current.profile?.id ?: return
        if (current.isLoadingBookmarkedListings || current.isLoadingMoreBookmarkedListings) return
        loadingJob = scope.launch { loadPages(userId, cursor, loadAll = false) }
    }

    fun updateQuery(query: String) {
        state.update { it.copy(bookmarkedListingFilters = it.bookmarkedListingFilters.copy(query = query)) }
        loadRemainingPagesForActiveFilters()
    }

    fun applyFilters(category: String?, condition: String?) {
        state.update { current ->
            current.copy(
                bookmarkedListingFilters = current.bookmarkedListingFilters.copy(
                    category = category,
                    condition = condition
                )
            )
        }
        loadRemainingPagesForActiveFilters()
    }

    fun remove(listingId: String) {
        val current = state.value
        val userId = current.profile?.id ?: return
        val removalUnavailable = current.removingBookmarkedListingId != null ||
            current.bookmarkedListings.none { it.id == listingId }
        if (removalUnavailable) return
        removalJob?.cancel()
        loadingJob?.cancel()
        removalJob = scope.launch {
            state.update { it.copy(removingBookmarkedListingId = listingId, errorMessage = null) }
            performRemoval(userId, listingId)
        }
    }

    private suspend fun loadInitialPage(userId: UserId) {
        try {
            val page = getPage(userId, null)
            state.update {
                it.copy(
                    bookmarkedListings = page.listings,
                    bookmarkedListingFilterOptions = page.filterOptions,
                    isLoadingBookmarkedListings = false,
                    bookmarkedListingsNextCursor = page.nextCursor,
                    selectedBookmarkedListingId = null,
                    errorMessage = null
                )
            }
            if (state.value.bookmarkedListingFilters.isActive && page.nextCursor != null) {
                loadPages(userId, page.nextCursor, loadAll = true)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            state.update {
                it.copy(
                    isLoadingBookmarkedListings = false,
                    isLoadingMoreBookmarkedListings = false,
                    bookmarkedListingsNextCursor = null,
                    errorMessage = "Unable to load bookmarked listings. Please try again."
                )
            }
        }
    }

    private fun loadRemainingPagesForActiveFilters() {
        val current = state.value
        val userId = current.profile?.id ?: return
        val cursor = current.bookmarkedListingsNextCursor ?: return
        if (!current.bookmarkedListingFilters.isActive || current.isLoadingBookmarkedListings) return
        loadingJob?.cancel()
        loadingJob = scope.launch { loadPages(userId, cursor, loadAll = true) }
    }

    private suspend fun loadPages(userId: UserId, initialCursor: ProfileBookmarksPageCursor, loadAll: Boolean) {
        var cursor: ProfileBookmarksPageCursor? = initialCursor
        state.update { it.copy(isLoadingMoreBookmarkedListings = true, errorMessage = null) }
        try {
            do {
                val requestedCursor = cursor ?: break
                val page = getPage(userId, requestedCursor)
                appendPage(page)
                cursor = page.nextCursor
                check(cursor == null || cursor != requestedCursor) { "Bookmark pagination did not advance." }
            } while (loadAll && cursor != null)
            state.update { it.copy(isLoadingMoreBookmarkedListings = false) }
        } catch (error: CancellationException) {
            state.update { it.copy(isLoadingMoreBookmarkedListings = false) }
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

    private fun appendPage(page: ProfileBookmarkedListingsPage) {
        state.update {
            it.copy(
                bookmarkedListings = (it.bookmarkedListings + page.listings).distinctBy { listing -> listing.id },
                bookmarkedListingFilterOptions = page.filterOptions,
                bookmarkedListingsNextCursor = page.nextCursor
            )
        }
    }

    private suspend fun performRemoval(userId: UserId, listingId: String) {
        try {
            val remover = dependencies.remover ?: error("Bookmark removal is unavailable.")
            remover.removeBookmark(userId, listingId)
            state.update {
                it.copy(
                    bookmarkedListings = it.bookmarkedListings.filterNot { listing -> listing.id == listingId },
                    bookmarkedListingsNextCursor = it.bookmarkedListingsNextCursor?.afterRemoval(),
                    removingBookmarkedListingId = null,
                    selectedBookmarkedListingId = null,
                    errorMessage = null
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            state.update {
                it.copy(
                    removingBookmarkedListingId = null,
                    errorMessage = "Unable to remove bookmark. Please try again."
                )
            }
        }
    }

    private suspend fun getPage(
        userId: UserId,
        cursor: ProfileBookmarksPageCursor?
    ): ProfileBookmarkedListingsPage = dependencies.provider?.getBookmarkedListings(
        userId,
        cursor,
        BOOKMARKS_PAGE_SIZE
    ) ?: ProfileBookmarkedListingsPage(emptyList(), null)

    private companion object {
        const val BOOKMARKS_PAGE_SIZE = 20
    }
}

private val ProfileListingFilterState.isActive: Boolean
    get() = query.isNotBlank() || category != null || condition != null

private fun ProfileBookmarksPageCursor.afterRemoval(): ProfileBookmarksPageCursor =
    ProfileBookmarksPageCursor((offset - 1).coerceAtLeast(0))
