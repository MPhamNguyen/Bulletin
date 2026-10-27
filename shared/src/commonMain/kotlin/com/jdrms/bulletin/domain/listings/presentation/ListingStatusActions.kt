package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.CreateListingErrorMessages
import com.jdrms.bulletin.domain.listings.application.CurrentListingSellerProvider
import com.jdrms.bulletin.domain.listings.application.MarkListingSold
import com.jdrms.bulletin.domain.listings.application.RecentListingBuyersProvider
import com.jdrms.bulletin.domain.listings.application.RestoreListingToMarketplace
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ListingStatusActions(
    private val state: MutableStateFlow<ListingsUiState>,
    private val scope: CoroutineScope,
    private val sellerProvider: CurrentListingSellerProvider,
    private val markListingSold: MarkListingSold?,
    private val recentBuyers: RecentListingBuyersProvider,
    private val restoreListing: RestoreListingToMarketplace?,
    private val callbacks: ListingActionCallbacks
) {
    private var recentBuyersJob: Job? = null

    fun requestMarkSold(listing: Listing) {
        recentBuyersJob?.cancel()
        state.update {
            it.copy(
                pendingSold = listing,
                recentSoldBuyers = emptyList(),
                selectedBuyerId = null,
                soldOnAnotherPlatform = true,
                errorMessage = null
            )
        }
        recentBuyersJob = scope.launch {
            val seller = sellerProvider.getCurrentSeller()
            val buyers = if (seller is Result.Success) {
                recentBuyers.getRecentBuyers(listing.id, seller.data.id.value)
            } else {
                emptyList()
            }
            if (state.value.pendingSold?.id != listing.id) return@launch
            state.update {
                it.copy(
                    recentSoldBuyers = buyers,
                    selectedBuyerId = buyers.singleOrNull()?.id,
                    soldOnAnotherPlatform = buyers.isEmpty(),
                    errorMessage = null
                )
            }
        }
    }

    fun cancelMarkSold() {
        recentBuyersJob?.cancel()
        state.update { it.copy(pendingSold = null, recentSoldBuyers = emptyList(), selectedBuyerId = null) }
    }

    fun selectBuyer(id: String?) = state.update {
        it.copy(selectedBuyerId = id, soldOnAnotherPlatform = false, errorMessage = null)
    }

    fun selectSoldElsewhere() = state.update {
        it.copy(selectedBuyerId = null, soldOnAnotherPlatform = true, errorMessage = null)
    }

    fun requestRestore(listing: Listing) = state.update { it.copy(pendingRestoration = listing, errorMessage = null) }

    fun cancelRestore() = state.update { it.copy(pendingRestoration = null) }

    fun confirmRestore() {
        val restore = restoreListing ?: return
        val listing = claimPending({ it.pendingRestoration }) { current ->
            current.copy(isRestoring = true, errorMessage = null)
        } ?: return
        scope.launch {
            when (val seller = sellerProvider.getCurrentSeller()) {
                is Result.Success -> when (val result = restore(listing.id, seller.data.id)) {
                    is Result.Success -> {
                        state.update { it.copy(pendingRestoration = null, isRestoring = false) }
                        callbacks.notify("Listing restored to marketplace!")
                        callbacks.reload(seller.data)
                        callbacks.listingChangedSignal?.emit()
                    }
                    is Result.Error -> state.update {
                        it.copy(
                            isRestoring = false,
                            errorMessage = CreateListingErrorMessages.toUserMessage(result.exception)
                        )
                    }
                }
                is Result.Error -> state.update {
                    it.copy(
                        isRestoring = false,
                        errorMessage = CreateListingErrorMessages.toUserMessage(seller.exception)
                    )
                }
            }
        }
    }

    fun confirmMarkSold() {
        val markSold = markListingSold ?: return
        val listing = claimPending({ it.pendingSold }) { current ->
            current.copy(isMarkingSold = true, errorMessage = null)
        } ?: return
        scope.launch { performMarkSold(markSold, listing) }
    }

    private suspend fun performMarkSold(useCase: MarkListingSold, listing: Listing) {
        when (val seller = sellerProvider.getCurrentSeller()) {
            is Result.Success -> {
                val buyerId = state.value.selectedBuyerId
                if (buyerId == null && !state.value.soldOnAnotherPlatform) {
                    state.update { it.copy(isMarkingSold = false, errorMessage = "Choose who bought this listing.") }
                    return
                }
                when (val result = useCase(listing.id, seller.data.id, buyerId)) {
                    is Result.Success -> {
                        state.update {
                            it.copy(
                                pendingSold = null,
                                recentSoldBuyers = emptyList(),
                                selectedBuyerId = null,
                                isMarkingSold = false
                            )
                        }
                        callbacks.notify("Listing marked as sold!")
                        callbacks.reload(seller.data)
                        callbacks.listingChangedSignal?.emit()
                    }
                    is Result.Error -> state.update {
                        it.copy(
                            isMarkingSold = false,
                            errorMessage = CreateListingErrorMessages.toUserMessage(result.exception)
                        )
                    }
                }
            }
            is Result.Error -> state.update {
                it.copy(
                    isMarkingSold = false,
                    errorMessage = CreateListingErrorMessages.toUserMessage(seller.exception)
                )
            }
        }
    }

    private fun claimPending(
        pending: (ListingsUiState) -> Listing?,
        claim: (ListingsUiState) -> ListingsUiState
    ): Listing? {
        while (true) {
            val current = state.value
            if (current.isRestoring || current.isMarkingSold) return null
            val listing = pending(current) ?: return null
            if (state.compareAndSet(current, claim(current))) return listing
        }
    }
}
