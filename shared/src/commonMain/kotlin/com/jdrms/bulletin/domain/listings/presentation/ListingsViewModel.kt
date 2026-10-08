package com.jdrms.bulletin.domain.listings.presentation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.generateUuid
import com.jdrms.bulletin.domain.listings.application.CreateListing
import com.jdrms.bulletin.domain.listings.application.CreateListingErrorMessages
import com.jdrms.bulletin.domain.listings.application.CurrentListingSellerProvider
import com.jdrms.bulletin.domain.listings.application.DeleteListing
import com.jdrms.bulletin.domain.listings.application.GetSellerListings
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.application.MarkListingSold
import com.jdrms.bulletin.domain.listings.application.RestoreListingToMarketplace
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingCondition
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingPrice
import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
class ListingsViewModel(
    private val createListing: CreateListing,
    private val manageListing: ManageListing,
    private val markListingSold: MarkListingSold? = null,
    private val restoreListingToMarketplace: RestoreListingToMarketplace? = null,
    private val deleteListing: DeleteListing,
    private val getSellerListings: GetSellerListings,
    private val currentSellerProvider: CurrentListingSellerProvider,
    private val listingChangedSignal: RefreshSignal? = null
) : ViewModel() {
    internal val _uiState = MutableStateFlow(ListingsUiState())
    val uiState: StateFlow<ListingsUiState> = _uiState.asStateFlow()
    internal var flashNotificationJob: Job? = null
    internal val notificationScope get() = viewModelScope
    init {
        loadMyListings()
    }
    fun loadMyListings(seller: ListingSeller? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(loadState = ListingsLoadState.LOADING) }
            when (val sellerResult = seller?.let { Result.Success(it) } ?: currentSellerProvider.getCurrentSeller()) {
                is Result.Success -> {
                    val listings = getSellerListings(sellerResult.data.id)
                    _uiState.update { it.copy(myListings = listings, loadState = ListingsLoadState.LOADED) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            loadState = ListingsLoadState.FAILED,
                            errorMessage = CreateListingErrorMessages.toUserMessage(sellerResult.exception)
                        )
                    }
                }
            }
        }
    }
    fun onTitleChanged(title: String) {
        _uiState.update { it.copy(newTitle = title, errorMessage = null, successMessage = null) }
    }
    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(newDescription = description, errorMessage = null, successMessage = null) }
    }
    fun onPriceChanged(price: String) {
        _uiState.update {
            it.copy(
                newPriceCents = normalizeCurrencyReplacement(price),
                errorMessage = null,
                successMessage = null
            )
        }
    }
    fun onCategorySelected(category: ListingCategory) {
        _uiState.update { it.copy(newCategory = category) }
    }
    fun onConditionSelected(condition: ListingCondition) {
        _uiState.update { it.copy(newCondition = condition) }
    }
    fun submitNewListing() {
        val draft = buildListingDraft() ?: return
        while (true) {
            val state = _uiState.value
            if (state.isSubmitting) return
            if (_uiState.compareAndSet(state, state.copy(isSubmitting = true, errorMessage = null))) break
        }
        viewModelScope.launch { submitListing(draft) }
    }
    private fun buildListingDraft(): NewListingDraft? {
        val state = _uiState.value
        val validation = validateNewListingDraft(state)
        validation.errorMessage?.let { message ->
            _uiState.update { it.copy(errorMessage = message) }
        }
        return validation.draft
    }
    private suspend fun submitListing(draft: NewListingDraft) {
        when (val sellerResult = currentSellerProvider.getCurrentSeller()) {
            is Result.Success -> {
                val seller = sellerResult.data
                val listing = Listing(
                    id = ListingId(generateUuid()),
                    sellerId = seller.id,
                    sellerName = seller.name,
                    title = draft.title,
                    description = draft.description,
                    price = ListingPrice(draft.price),
                    category = draft.category,
                    condition = draft.condition,
                    status = ListingStatus.AVAILABLE,
                    createdAtMillis = currentTimeMillis()
                )
                when (val result = createListing(listing)) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                newTitle = "",
                                newDescription = "",
                                newPriceCents = "",
                                isSubmitting = false
                            )
                        }
                        showFlashNotification("Listing posted successfully!")
                        loadMyListings(seller)
                        listingChangedSignal?.emit()
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = CreateListingErrorMessages.toUserMessage(result.exception)
                        )
                    }
                }
            }
            is Result.Error -> _uiState.update {
                it.copy(
                    isSubmitting = false,
                    errorMessage = CreateListingErrorMessages.toUserMessage(sellerResult.exception)
                )
            }
        }
    }
    fun requestDeleteListing(listing: Listing) {
        _uiState.update { it.copy(pendingDeletion = listing, errorMessage = null) }
    }
    fun cancelDeleteListing() {
        _uiState.update { it.copy(pendingDeletion = null) }
    }

    fun requestMarkListingSold(listing: Listing) {
        _uiState.update { it.copy(pendingSold = listing, errorMessage = null) }
    }

    fun cancelMarkListingSold() {
        _uiState.update { it.copy(pendingSold = null) }
    }

    fun requestRestoreListing(listing: Listing) {
        _uiState.update { it.copy(pendingRestoration = listing, errorMessage = null) }
    }

    fun cancelRestoreListing() {
        _uiState.update { it.copy(pendingRestoration = null) }
    }

    fun confirmRestoreListing() {
        val restore = restoreListingToMarketplace ?: return
        var pendingListing: Listing? = null
        while (pendingListing == null) {
            val state = _uiState.value
            val listing = state.pendingRestoration ?: return
            if (state.isRestoring) return
            if (_uiState.compareAndSet(state, state.copy(isRestoring = true, errorMessage = null))) {
                pendingListing = listing
            }
        }
        val listing = checkNotNull(pendingListing)
        viewModelScope.launch {
            when (val sellerResult = currentSellerProvider.getCurrentSeller()) {
                is Result.Success -> when (
                    val result = restore(listing.id, sellerResult.data.id)
                ) {
                    is Result.Success -> {
                        _uiState.update { it.copy(pendingRestoration = null, isRestoring = false) }
                        showFlashNotification("Listing restored to marketplace!")
                        loadMyListings(sellerResult.data)
                        listingChangedSignal?.emit()
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(
                            isRestoring = false,
                            errorMessage = CreateListingErrorMessages.toUserMessage(result.exception)
                        )
                    }
                }
                is Result.Error -> _uiState.update {
                    it.copy(
                        isRestoring = false,
                        errorMessage = CreateListingErrorMessages.toUserMessage(sellerResult.exception)
                    )
                }
            }
        }
    }

    fun confirmMarkListingSold() {
        val markSold = markListingSold ?: return
        var pendingListing: Listing? = null
        while (pendingListing == null) {
            val state = _uiState.value
            val listing = state.pendingSold ?: return
            if (state.isMarkingSold) return
            if (_uiState.compareAndSet(state, state.copy(isMarkingSold = true, errorMessage = null))) {
                pendingListing = listing
            }
        }
        val listing = checkNotNull(pendingListing)
        viewModelScope.launch {
            when (val sellerResult = currentSellerProvider.getCurrentSeller()) {
                is Result.Success -> {
                    when (val result = markSold(listing.id, sellerResult.data.id)) {
                        is Result.Success -> {
                            _uiState.update { it.copy(pendingSold = null, isMarkingSold = false) }
                            showFlashNotification("Listing marked as sold!")
                            loadMyListings(sellerResult.data)
                            listingChangedSignal?.emit()
                        }
                        is Result.Error -> _uiState.update {
                            it.copy(
                                isMarkingSold = false,
                                errorMessage = CreateListingErrorMessages.toUserMessage(result.exception)
                            )
                        }
                    }
                }
                is Result.Error -> _uiState.update {
                    it.copy(
                        isMarkingSold = false,
                        errorMessage = CreateListingErrorMessages.toUserMessage(sellerResult.exception)
                    )
                }
            }
        }
    }

    fun confirmDeleteListing() {
        var pendingListing: Listing? = null
        while (pendingListing == null) {
            val state = _uiState.value
            val listing = state.pendingDeletion ?: return
            if (state.isDeleting) return
            if (_uiState.compareAndSet(state, state.copy(isDeleting = true, errorMessage = null))) {
                pendingListing = listing
            }
        }
        val listing = checkNotNull(pendingListing)
        viewModelScope.launch {
            when (val sellerResult = currentSellerProvider.getCurrentSeller()) {
                is Result.Success -> {
                    when (val result = deleteListing(listing.id, sellerResult.data.id)) {
                        is Result.Success -> {
                            _uiState.update { it.copy(pendingDeletion = null, isDeleting = false) }
                            showFlashNotification("Listing deleted successfully!")
                            loadMyListings(sellerResult.data)
                            listingChangedSignal?.emit()
                        }
                        is Result.Error -> _uiState.update {
                            it.copy(
                                isDeleting = false,
                                errorMessage = CreateListingErrorMessages.toUserMessage(result.exception)
                            )
                        }
                    }
                }
                is Result.Error -> _uiState.update {
                    it.copy(
                        isDeleting = false,
                        errorMessage = CreateListingErrorMessages.toUserMessage(sellerResult.exception)
                    )
                }
            }
        }
    }
    fun startEditing(listing: Listing) {
        flashNotificationJob?.cancel()
        viewModelScope.launch {
            when (val sellerResult = currentSellerProvider.getCurrentSeller()) {
                is Result.Success -> {
                    val seller = sellerResult.data
                    if (!listing.isOwnedBy(seller.id)) {
                        _uiState.update {
                            it.copy(errorMessage = "Only the owner can edit this listing.")
                        }
                        return@launch
                    }
                    _uiState.update {
                        it.copy(
                            editingListing = listing,
                            editTitle = listing.title,
                            editDescription = listing.description,
                            editPriceCents = amountToCurrencyDigits(listing.price.amount),
                            editCategory = listing.category,
                            editCondition = listing.condition,
                            errorMessage = null,
                            successMessage = null
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = CreateListingErrorMessages.toUserMessage(sellerResult.exception))
                    }
                }
            }
        }
    }
    fun cancelEditing() {
        flashNotificationJob?.cancel()
        _uiState.update {
            it.copy(
                editingListing = null,
                editTitle = "",
                editDescription = "",
                editPriceCents = "",
                isUpdating = false,
                errorMessage = null,
                successMessage = null
            )
        }
    }
    fun onEditTitleChanged(title: String) {
        _uiState.update { it.copy(editTitle = title, errorMessage = null, successMessage = null) }
    }
    fun onEditDescriptionChanged(description: String) {
        _uiState.update { it.copy(editDescription = description, errorMessage = null, successMessage = null) }
    }
    fun onEditPriceChanged(price: String) {
        _uiState.update {
            it.copy(
                editPriceCents = normalizeCurrencyReplacement(price),
                errorMessage = null,
                successMessage = null
            )
        }
    }
    fun onEditCategorySelected(category: ListingCategory) {
        _uiState.update { it.copy(editCategory = category) }
    }
    fun onEditConditionSelected(condition: ListingCondition) {
        _uiState.update { it.copy(editCondition = condition) }
    }
    fun saveListingChanges() {
        val draft = validateEditDraft() ?: return
        while (true) {
            val state = _uiState.value
            if (state.isUpdating) return
            if (_uiState.compareAndSet(state, state.copy(isUpdating = true, errorMessage = null))) break
        }
        viewModelScope.launch { applyListingUpdate(draft) }
    }
    private fun validateEditDraft(): ValidatedEditDraft? {
        val currentListing = _uiState.value.editingListing ?: return null
        val state = _uiState.value
        val validationError = when {
            state.editPriceCents.isEmpty() ->
                "Please enter a price"
            state.editTitle.trim().length < 3 ->
                "Title must be at least 3 characters"
            state.editDescription.trim().isBlank() ->
                "Description cannot be empty"
            else -> null
        }
        return if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            null
        } else {
            ValidatedEditDraft(
                currentListing = currentListing,
                title = state.editTitle.trim(),
                description = state.editDescription.trim(),
                price = currencyDigitsToAmount(state.editPriceCents),
                category = state.editCategory,
                condition = state.editCondition
            )
        }
    }
    private suspend fun applyListingUpdate(draft: ValidatedEditDraft) {
        when (val sellerResult = currentSellerProvider.getCurrentSeller()) {
            is Result.Success -> {
                val seller = sellerResult.data
                if (!draft.currentListing.isOwnedBy(seller.id)) {
                    _uiState.update {
                        it.copy(isUpdating = false, errorMessage = "Only the owner can edit this listing.")
                    }
                    return
                }
                executeListingUpdate(draft, seller)
            }
            is Result.Error -> {
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        errorMessage = CreateListingErrorMessages.toUserMessage(sellerResult.exception)
                    )
                }
            }
        }
    }
    private suspend fun executeListingUpdate(draft: ValidatedEditDraft, seller: ListingSeller) {
        val updatedDetails = runCatching {
            draft.currentListing.updateDetails(
                editorSellerId = seller.id,
                title = draft.title,
                description = draft.description,
                price = ListingPrice(draft.price),
                category = draft.category,
                condition = draft.condition
            )
        }
        if (updatedDetails.isFailure) {
            _uiState.update {
                it.copy(
                    isUpdating = false,
                    errorMessage = updatedDetails.exceptionOrNull()?.message ?: "Validation failed"
                )
            }
            return
        }
        val updated = updatedDetails.getOrThrow()
        when (val result = manageListing.updateListing(updated, seller.id)) {
            is Result.Success -> {
                _uiState.update {
                    it.copy(
                        editingListing = null,
                        isUpdating = false
                    )
                }
                showFlashNotification("Listing updated successfully!")
                loadMyListings(seller)
                listingChangedSignal?.emit()
            }
            is Result.Error -> {
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        errorMessage = CreateListingErrorMessages.toUserMessage(result.exception)
                    )
                }
            }
        }
    }
    fun clearMessages() {
        flashNotificationJob?.cancel()
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
    companion object {
        const val FLASH_NOTIFICATION_DURATION_MILLIS = 3_000L
    }
}
