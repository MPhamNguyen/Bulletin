package com.jdrms.bulletin.domain.listings.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@Composable
fun MyListingsScreen(
    viewModel: ListingsViewModel,
    onBack: () -> Unit,
    onEditListing: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler {
        if (uiState.editingListing != null) viewModel.cancelEditing()
        onBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearMessages()
            viewModel.cancelEditing()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.editingListing != null) {
            EditListingView(
                uiState = uiState,
                actions = EditListingActions(
                    onTitleChange = viewModel::onEditTitleChanged,
                    onPriceChange = viewModel::onEditPriceChanged,
                    onCategorySelected = viewModel::onEditCategorySelected,
                    onConditionSelected = viewModel::onEditConditionSelected,
                    onDescriptionChange = viewModel::onEditDescriptionChanged,
                    onImagesSelected = viewModel::addEditImages,
                    onImageSelectionError = viewModel::imageSelectionFailed,
                    onRemoveImage = viewModel::removeEditImage,
                    onCropImage = viewModel::updateEditImageCrop,
                    onAttributeChange = viewModel::onEditAttributeChanged,
                    onConfirmCategoryChange = viewModel::confirmCategoryChange,
                    onCancelCategoryChange = viewModel::cancelCategoryChange,
                    onSave = viewModel::saveListingChanges,
                    onBack = {
                        viewModel.cancelEditing()
                        onBack()
                    }
                )
            )
        } else {
            MyListingsListView(
                uiState = uiState,
                actions = MyListingsListActions(
                    onBack = {
                        viewModel.clearMessages()
                        onBack()
                    },
                    onEditListing = { listing ->
                        viewModel.startEditing(listing)
                        onEditListing()
                    },
                    onDeleteListing = viewModel::requestDeleteListing,
                    onMarkSoldListing = viewModel::requestMarkListingSold,
                    onRestoreListing = viewModel::requestRestoreListing,
                    onRetryReviewRequestCancellation = viewModel::retryCancelReviewRequests
                )
            )
        }

        uiState.pendingDeletion?.let { listing ->
            ListingDeleteConfirmationDialog(
                listing = listing,
                isDeleting = uiState.isDeleting,
                errorMessage = uiState.errorMessage,
                onDismiss = viewModel::cancelDeleteListing,
                onConfirm = viewModel::confirmDeleteListing
            )
        }
        uiState.pendingSold?.let { listing ->
            ListingSoldConfirmationDialog(
                state = ListingSoldConfirmationState(
                    listing = listing,
                    isMarkingSold = uiState.isMarkingSold,
                    recentBuyers = uiState.recentSoldBuyers,
                    selectedBuyerId = uiState.selectedBuyerId,
                    soldOnAnotherPlatform = uiState.soldOnAnotherPlatform,
                    errorMessage = uiState.errorMessage
                ),
                actions = ListingSoldConfirmationActions(
                    onDismiss = viewModel::cancelMarkListingSold,
                    onConfirm = viewModel::confirmMarkListingSold,
                    onBuyerSelected = viewModel::selectSoldBuyer,
                    onSoldOnAnotherPlatform = viewModel::selectSoldOnAnotherPlatform
                )
            )
        }
        uiState.pendingRestoration?.let { listing ->
            ListingRestoreConfirmationDialog(
                listing = listing,
                isRestoring = uiState.isRestoring,
                errorMessage = uiState.errorMessage,
                onDismiss = viewModel::cancelRestoreListing,
                onConfirm = viewModel::confirmRestoreListing
            )
        }
    }
}
