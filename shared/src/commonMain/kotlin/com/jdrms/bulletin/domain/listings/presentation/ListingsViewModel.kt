package com.jdrms.bulletin.domain.listings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.ImageCropSettings
import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.designsystem.normalizeCurrencyReplacement
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
            callbacks = listingActionCallbacks,
            attributeActions = attributeActions
        )
    }
    private val photoActions by lazy { ListingPhotoActions(_uiState) }
    private val attributeActions by lazy { ListingAttributeDraftActions(_uiState) }
    private val editActions by lazy {
        ListingEditActions(
            state = _uiState,
            scope = viewModelScope,
            manageListing = manageListing,
            manageImages = manageListingImages,
            sellerProvider = currentSellerProvider,
            callbacks = listingActionCallbacks,
            attributeActions = attributeActions
        )
    }
    private val listingActionCallbacks by lazy {
        ListingActionCallbacks(
            listingChangedSignal,
            ::showFlashNotification,
            ::loadMyListings,
            cancelNotification = { flashNotificationJob?.cancel() }
        )
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
    fun onCategorySelected(category: ListingCategory) =
        attributeActions.selectCategory(ListingForm.NEW, category)
    fun onNewAttributeChanged(key: String, text: String) =
        attributeActions.inputChanged(ListingForm.NEW, key, text)
    fun onConditionSelected(condition: ListingCondition) {
        _uiState.update { it.copy(newCondition = condition) }
    }
    fun addNewImages(images: List<ListingImageUpload>) = photoActions.addNew(images)
    fun removeNewImage(index: Int) = photoActions.removeNew(index)
    fun updateNewImageCrop(index: Int, crop: ImageCropSettings) =
        photoActions.updateNewCrop(index, crop)
    fun updateEditImageCrop(index: Int, crop: ImageCropSettings) =
        photoActions.updateEditCrop(index, crop)
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
    fun startEditing(listing: Listing) = editActions.start(listing)
    fun cancelEditing() = editActions.cancel()
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
    fun onEditCategorySelected(category: ListingCategory) =
        attributeActions.selectCategory(ListingForm.EDIT, category)
    fun onEditAttributeChanged(key: String, text: String) =
        attributeActions.inputChanged(ListingForm.EDIT, key, text)
    fun confirmCategoryChange() = attributeActions.confirmCategoryChange()
    fun cancelCategoryChange() = attributeActions.cancelCategoryChange()
    fun onEditConditionSelected(condition: ListingCondition) {
        _uiState.update { it.copy(editCondition = condition) }
    }
    fun saveListingChanges() = editActions.save()
    fun clearMessages() {
        flashNotificationJob?.cancel()
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
    companion object {
        const val FLASH_NOTIFICATION_DURATION_MILLIS = 3_000L
    }
}
