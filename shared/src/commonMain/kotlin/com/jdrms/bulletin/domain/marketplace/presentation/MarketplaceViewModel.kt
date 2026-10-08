package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MarketplaceViewModel(
    private val searchMarketplace: SearchMarketplace,
    bookmarks: MarketplaceBookmarkDependencies,
    private val viewMarketplaceListing: ViewMarketplaceListing,
    private val listingChangedSignal: RefreshSignal? = null,
    private val currentUserIdProvider: suspend () -> String? = { DEFAULT_USER_ID }
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketplaceUiState())
    val uiState: StateFlow<MarketplaceUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null
    private val firstPageLoader = MarketplaceFirstPageLoader(
        _uiState,
        searchMarketplace,
        bookmarks,
        currentUserIdProvider
    )
    private val bookmarkController = MarketplaceBookmarkController(
        _uiState,
        viewModelScope,
        bookmarks,
        currentUserIdProvider
    )

    init {
        refreshListings()
        viewModelScope.launch {
            listingChangedSignal?.events?.collect {
                refreshListings()
                _uiState.value.selectedListingId?.let(::loadListingDetail)
            }
        }
    }

    fun refreshListings(userId: String? = null) {
        loadFirstPage(userId = userId, debounceMillis = 0L)
    }

    private fun loadFirstPage(userId: String?, debounceMillis: Long) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            firstPageLoader.load(userId, debounceMillis)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadFirstPage(userId = null, debounceMillis = SEARCH_DEBOUNCE_MILLIS)
    }

    fun onCategorySelected(category: MarketplaceCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
        loadFirstPage(userId = null, debounceMillis = 0L)
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        val cursor = currentState.nextCursor ?: return
        if (currentState.isLoading || currentState.isLoadingMore || currentState.endReached) return

        _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val query = currentState.searchQuery
            val category = currentState.selectedCategory
            runCatching {
                searchMarketplace.getPage(
                    MarketplacePageRequest(
                        query = query,
                        category = category,
                        cursor = cursor
                    )
                )
            }.fold(
                onSuccess = { page ->
                    _uiState.update { state ->
                        if (
                            state.searchQuery == query &&
                            state.selectedCategory == category &&
                            state.nextCursor == cursor
                        ) {
                            state.copy(
                                items = (state.items + page.items).distinctBy { it.id },
                                isLoadingMore = false,
                                nextCursor = page.nextCursor,
                                endReached = page.nextCursor == null
                            )
                        } else {
                            state
                        }
                    }
                },
                onFailure = { error ->
                    error.rethrowCancellation()
                    _uiState.update { state ->
                        if (
                            state.searchQuery == query &&
                            state.selectedCategory == category &&
                            state.nextCursor == cursor
                        ) {
                            state.copy(
                                isLoadingMore = false,
                                errorMessage = "Unable to load more listings. Please try again."
                            )
                        } else {
                            state
                        }
                    }
                }
            )
        }
    }

    fun retryListings() {
        if (_uiState.value.items.isEmpty()) {
            refreshListings()
        } else {
            loadNextPage()
        }
    }

    fun onListingClicked(listingId: String) {
        _uiState.update {
            it.copy(
                selectedListingId = listingId,
                selectedListing = null,
                isDetailLoading = true,
                detailErrorMessage = null,
                isDetailSheetOpen = true
            )
        }
        loadListingDetail(listingId)
    }

    fun retryLoadListingDetail() {
        val listingId = _uiState.value.selectedListingId ?: return
        _uiState.update {
            it.copy(isDetailLoading = true, detailErrorMessage = null)
        }
        loadListingDetail(listingId)
    }

    private fun loadListingDetail(listingId: String) {
        viewModelScope.launch {
            when (val result = viewMarketplaceListing(listingId)) {
                is com.jdrms.bulletin.core.common.Result.Success -> {
                    val listing = result.data
                    val canonicalId = MarketplaceItemId(listingId)
                    val isBookmarked = _uiState.value.bookmarkedItemIds.contains(canonicalId)
                    _uiState.update {
                        it.copy(
                            selectedListing = listing.copy(
                                id = canonicalId,
                                isBookmarked = isBookmarked
                            ),
                            isDetailLoading = false,
                            detailErrorMessage = null
                        )
                    }
                }
                is com.jdrms.bulletin.core.common.Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isDetailLoading = false,
                            detailErrorMessage = result.exception.message ?: "Failed to load listing details."
                        )
                    }
                }
            }
        }
    }

    fun dismissListingDetail() {
        _uiState.update {
            it.copy(
                isDetailSheetOpen = false,
                selectedListing = null,
                selectedListingId = null,
                detailErrorMessage = null,
                isDetailLoading = false
            )
        }
    }

    fun onSellerClicked(sellerId: String) {
        _uiState.update {
            it.copy(
                isDetailSheetOpen = false,
                isSellerProfileOpen = true,
                isSellerProfileLoading = true,
                sellerProfileErrorMessage = null,
                selectedSellerProfile = null
            )
        }
        viewModelScope.launch {
            try {
                val profile = viewMarketplaceListing.getSellerProfile(sellerId)
                _uiState.update {
                    it.copy(
                        isSellerProfileLoading = false,
                        sellerProfileErrorMessage = if (profile == null) {
                            "Seller profile is currently unavailable."
                        } else {
                            null
                        },
                        selectedSellerProfile = profile
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSellerProfileLoading = false,
                        sellerProfileErrorMessage = "Unable to load the seller profile. Please try again."
                    )
                }
            }
        }
    }

    fun dismissSellerProfile() {
        _uiState.update {
            it.copy(
                isSellerProfileOpen = false,
                selectedSellerProfile = null,
                sellerProfileErrorMessage = null,
                isDetailSheetOpen = it.selectedListing != null
            )
        }
    }

    fun toggleBookmark(itemId: MarketplaceItemId, userId: String? = null) {
        bookmarkController.toggle(itemId, userId)
    }

    private companion object {
        const val DEFAULT_USER_ID = "student_user"
        const val SEARCH_DEBOUNCE_MILLIS = 300L
    }
}
