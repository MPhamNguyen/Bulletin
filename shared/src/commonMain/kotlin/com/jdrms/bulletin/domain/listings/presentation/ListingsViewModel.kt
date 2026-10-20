package com.jdrms.bulletin.domain.listings.presentation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.domain.listings.application.CreateListing
import com.jdrms.bulletin.domain.listings.application.CreateListingErrorMessages
import com.jdrms.bulletin.domain.listings.application.CurrentListingSellerProvider
import com.jdrms.bulletin.domain.listings.application.DeleteListing
import com.jdrms.bulletin.domain.listings.application.GetSellerListings
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.application.ManageListingImages
import com.jdrms.bulletin.domain.listings.application.MarkListingSold
import com.jdrms.bulletin.domain.listings.application.RecentListingBuyersProvider
import com.jdrms.bulletin.domain.listings.application.RestoreListingToMarketplace
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingCondition
import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload
import com.jdrms.bulletin.domain.listings.domain.model.ListingPrice
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
data class ListingsViewModelSessionDependencies(
    val currentSellerProvider: CurrentListingSellerProvider,
    val listingChangedSignal: RefreshSignal? = null,
    val manageListingImages: ManageListingImages? = null
)

class ListingsViewModel(
    private val createListing: CreateListing,
    private val manageListing: ManageListing,
    private val markListingSold: MarkListingSold? = null,
    private val recentListingBuyersProvider: RecentListingBuyersProvider =
        RecentListingBuyersProvider { _, _ -> emptyList() },
    private val restoreListingToMarketplace: RestoreListingToMarketplace? = null,
    private val deleteListing: DeleteListing,
    private val getSellerListings: GetSellerListings,
    private val session: ListingsViewModelSessionDependencies
) : ViewModel() {
    private val currentSellerProvider get() = session.currentSellerProvider
    private val listingChangedSignal get() = session.listingChangedSignal
    private val manageListingImages get() = session.manageListingImages
    internal val _uiState = MutableStateFlow(ListingsUiState())
    val uiState: StateFlow<ListingsUiState> = _uiState.asStateFlow()
    internal var flashNotificationJob: Job? = null
    internal val notificationScope get() = viewModelScope
    private val statusActions by lazy {
        ListingStatusActions(
            state = _uiState,
            scope = viewModelScope,
            sellerProvider = currentSellerProvider,
            markListingSold = markListingSold,
            recentBuyers = recentListingBuyersProvider,
            restoreListing = restoreListingToMarketplace,
            callbacks = listingActionCallbacks
        )
    }
    private val creationActions by lazy {
        ListingCreationActions(
            state = _uiState,
            scope = viewModelScope,
            createListing = createListing,
            manageListing = manageListing,
            manageImages = manageListingImages,
            sellerProvider = currentSellerProvider,
            callbacks = listingActionCallbacks
        )
    }
    private val photoActions by lazy { ListingPhotoActions(_uiState) }
    private val listingActionCallbacks by lazy {
        ListingActionCallbacks(listingChangedSignal, ::showFlashNotification, ::loadMyListings)
    }
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
    fun addNewImages(images: List<ListingImageUpload>) = photoActions.addNew(images)
    fun removeNewImage(index: Int) = photoActions.removeNew(index)
    fun addEditImages(images: List<ListingImageUpload>) = photoActions.addEdit(images)
    fun removeEditImage(index: Int) = photoActions.removeEdit(index)
    fun imageSelectionFailed(message: String) = photoActions.failed(message)
    fun submitNewListing() = creationActions.submit()
    fun requestDeleteListing(listing: Listing) {
        _uiState.update { it.copy(pendingDeletion = listing, errorMessage = null) }
    }
    fun cancelDeleteListing() {
        _uiState.update { it.copy(pendingDeletion = null) }
    }

    fun requestMarkListingSold(listing: Listing) = statusActions.requestMarkSold(listing)
    fun cancelMarkListingSold() = statusActions.cancelMarkSold()
    fun selectSoldBuyer(id: String?) = statusActions.selectBuyer(id)
    fun selectSoldOnAnotherPlatform() = statusActions.selectSoldElsewhere()
    fun requestRestoreListing(listing: Listing) = statusActions.requestRestore(listing)
    fun cancelRestoreListing() = statusActions.cancelRestore()
    fun confirmRestoreListing() = statusActions.confirmRestore()
    fun confirmMarkListingSold() = statusActions.confirmMarkSold()

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
                            editPhotos = listing.images.map { ListingPhotoDraft.Existing(it) },
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
                editPhotos = emptyList(),
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
                condition = state.editCondition,
                photos = state.editPhotos
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
                condition = draft.condition,
                updatedAtMillis = currentTimeMillis()
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
                if (!synchronizeEditedImages(draft, seller, updated)) return
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

    private suspend fun synchronizeEditedImages(
        draft: ValidatedEditDraft,
        seller: ListingSeller,
        listing: Listing
    ): Boolean {
        val retained = draft.photos.filterIsInstance<ListingPhotoDraft.Existing>().map { it.url }
        val uploads = draft.photos.filterIsInstance<ListingPhotoDraft.Pending>().map { it.upload }
        if (draft.currentListing.images.isEmpty() && uploads.isEmpty()) return true
        val result = manageListingImages?.synchronize(listing.id, seller.id, retained, uploads)
            ?: Result.Error(IllegalStateException("Listing image updates are not configured."))
        return when (result) {
            is Result.Success -> {
                manageListing.updateListing(listing.copy(images = result.data), seller.id)
                true
            }
            is Result.Error -> {
                _uiState.update {
                    it.copy(
                        isUpdating = false,
                        errorMessage = "Listing details were saved, but image changes failed: " +
                            result.exception.message
                    )
                }
                false
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
