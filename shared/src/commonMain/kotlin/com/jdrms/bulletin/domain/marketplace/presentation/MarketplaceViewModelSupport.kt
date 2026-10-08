package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.BookmarkMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.application.GetMarketplaceListingBookmarks
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceItemPage
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.application.RemoveMarketplaceListingBookmark
import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MarketplaceBookmarkDependencies(
    val add: BookmarkMarketplaceListing,
    val remove: RemoveMarketplaceListingBookmark,
    val get: GetMarketplaceListingBookmarks
)

internal class MarketplaceFirstPageLoader(
    private val state: MutableStateFlow<MarketplaceUiState>,
    private val search: SearchMarketplace,
    private val bookmarks: MarketplaceBookmarkDependencies,
    private val currentUserId: suspend () -> String?
) {
    suspend fun load(userId: String?, debounceMillis: Long) {
        prepareForLoad()
        if (debounceMillis > 0L) delay(debounceMillis)
        val query = state.value.searchQuery
        val category = state.value.selectedCategory
        runCatching {
            val page = search.getPage(MarketplacePageRequest(query = query, category = category))
            val resolvedUserId = userId ?: currentUserId()
            val bookmarkedIds = resolvedUserId?.let { bookmarks.get.getBookmarkedIds(it) }.orEmpty()
            FirstPageResult(page, bookmarkedIds)
        }.fold(
            onSuccess = { applySuccess(it, query, category) },
            onFailure = { applyFailure(it, query, category) }
        )
    }

    private fun prepareForLoad() {
        state.update {
            it.copy(
                items = emptyList(),
                isLoading = true,
                isLoadingMore = false,
                nextCursor = null,
                endReached = false,
                errorMessage = null
            )
        }
    }

    private fun applySuccess(result: FirstPageResult, query: String, category: MarketplaceCategory?) {
        state.update { current ->
            if (current.searchQuery != query || current.selectedCategory != category) return@update current
            current.copy(
                items = result.page.items,
                bookmarkedItemIds = result.bookmarkedIds,
                isLoading = false,
                nextCursor = result.page.nextCursor,
                endReached = result.page.nextCursor == null
            )
        }
    }

    private fun applyFailure(error: Throwable, query: String, category: MarketplaceCategory?) {
        error.rethrowCancellation()
        state.update { current ->
            if (current.searchQuery != query || current.selectedCategory != category) return@update current
            current.copy(
                isLoading = false,
                isLoadingMore = false,
                errorMessage = "Unable to load marketplace listings. Please try again."
            )
        }
    }
}

private data class FirstPageResult(
    val page: MarketplaceItemPage,
    val bookmarkedIds: Set<MarketplaceItemId>
)

internal class MarketplaceBookmarkController(
    private val state: MutableStateFlow<MarketplaceUiState>,
    private val scope: CoroutineScope,
    private val dependencies: MarketplaceBookmarkDependencies,
    private val currentUserId: suspend () -> String?
) {
    private val pendingIds = mutableSetOf<MarketplaceItemId>()

    fun toggle(itemId: MarketplaceItemId, userId: String?) {
        if (!pendingIds.add(itemId)) return
        scope.launch {
            try {
                updateBookmark(itemId, userId)
            } finally {
                pendingIds.remove(itemId)
            }
        }
    }

    private suspend fun updateBookmark(itemId: MarketplaceItemId, userId: String?) {
        val resolvedUserId = userId ?: currentUserId()
        if (resolvedUserId == null) {
            state.update { it.copy(bookmarkErrorMessage = "Sign in to bookmark listings.") }
            return
        }
        val shouldBookmark = itemId !in state.value.bookmarkedItemIds
        val result = runCatching {
            if (shouldBookmark) {
                dependencies.add(resolvedUserId, itemId)
            } else {
                dependencies.remove(resolvedUserId, itemId)
            }
        }.getOrElse { error ->
            error.rethrowCancellation()
            Result.Error(error)
        }
        when (result) {
            is Result.Success -> applySuccessfulUpdate(itemId, shouldBookmark, resolvedUserId)
            is Result.Error -> state.update {
                it.copy(bookmarkErrorMessage = "Unable to update bookmark. Please try again.")
            }
        }
    }

    private suspend fun applySuccessfulUpdate(itemId: MarketplaceItemId, bookmarked: Boolean, userId: String) {
        state.update { current ->
            val ids = if (bookmarked) current.bookmarkedItemIds + itemId else current.bookmarkedItemIds - itemId
            val listing = current.selectedListing?.let {
                if (it.id == itemId) it.copy(isBookmarked = bookmarked) else it
            }
            current.copy(bookmarkedItemIds = ids, selectedListing = listing, bookmarkErrorMessage = null)
        }
        runCatching { dependencies.get.getBookmarkedIds(userId) }
            .onSuccess { ids ->
                state.update { current ->
                    current.copy(
                        bookmarkedItemIds = ids,
                        selectedListing = current.selectedListing?.let {
                            it.copy(isBookmarked = it.id in ids)
                        }
                    )
                }
            }
            .onFailure { it.rethrowCancellation() }
    }
}

internal fun Throwable.rethrowCancellation() {
    if (this is CancellationException) throw this
}
