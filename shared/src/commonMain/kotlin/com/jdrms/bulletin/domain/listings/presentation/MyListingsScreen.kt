package com.jdrms.bulletin.domain.listings.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.ui.zIndex

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
                onBack = {
                    viewModel.clearMessages()
                    onBack()
                },
                onEditListing = { listing ->
                    viewModel.startEditing(listing)
                    onEditListing()
                },
                onDeleteListing = viewModel::requestDeleteListing,
                onMarkSoldListing = viewModel::requestMarkListingSold
            )
        }

        AnimatedContent(
            targetState = uiState.successMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp, start = 24.dp, end = 24.dp)
                .zIndex(1f),
            transitionSpec = {
                (slideInVertically(initialOffsetY = { -it }) + fadeIn()) togetherWith
                    (slideOutVertically(targetOffsetY = { -it }) + fadeOut())
            },
            label = "Listing flash notification"
        ) { message ->
            if (message != null) ListingFlashMessage(message = message)
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
            AlertDialog(
                onDismissRequest = { if (!uiState.isMarkingSold) viewModel.cancelMarkListingSold() },
                title = { Text("Mark listing as sold?") },
                text = { Text("${listing.title} will be removed from marketplace results.") },
                dismissButton = {
                    TextButton(
                        onClick = viewModel::cancelMarkListingSold,
                        enabled = !uiState.isMarkingSold
                    ) { Text("Cancel") }
                },
                confirmButton = {
                    Button(
                        onClick = viewModel::confirmMarkListingSold,
                        enabled = !uiState.isMarkingSold,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (uiState.isMarkingSold) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        } else {
                            Text("Mark as sold")
                        }
                    }
                }
            )
        }
    }
}
