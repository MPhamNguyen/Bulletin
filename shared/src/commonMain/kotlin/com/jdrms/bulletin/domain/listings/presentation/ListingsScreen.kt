package com.jdrms.bulletin.domain.listings.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ListingsScreen(viewModel: ListingsViewModel, onClose: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsState()
    val pickListingImages = rememberListingImagePicker(viewModel::addNewImages, viewModel::imageSelectionFailed)
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val step = CreateListingStep.entries[stepIndex]
    val scrollState = rememberScrollState()

    BackHandler { if (step.isFirst) onClose() else stepIndex-- }
    LaunchedEffect(stepIndex) { scrollState.scrollTo(0) }
    // A rejected attribute value lives on the details step, so take the seller there.
    LaunchedEffect(state.newAttributes.errors) {
        if (state.newAttributes.errors.isNotEmpty()) stepIndex = CreateListingStep.DETAILS.ordinal
    }
    // The view model clears the title only after a successful post; start the next listing at step one.
    val wasSubmitting = remember { booleanArrayOf(false) }
    LaunchedEffect(state.isSubmitting) {
        if (wasSubmitting[0] && !state.isSubmitting && state.newTitle.isEmpty()) stepIndex = 0
        wasSubmitting[0] = state.isSubmitting
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            CreateListingHeader(step, onClose)
            Column(
                modifier = Modifier.weight(1f).verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                CreateListingStepContent(step, state, viewModel, pickListingImages)
            }
            CreateListingFooter(
                state = CreateListingFooterState(step, step.canContinue(state), state.isSubmitting),
                onBack = { stepIndex-- },
                onContinue = { if (step.isLast) viewModel.submitNewListing() else stepIndex++ }
            )
        }
        state.successMessage?.let {
            ListingFlashMessage(
                message = it,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp, start = 24.dp, end = 24.dp)
            )
        }
    }

    state.pendingCategoryChange?.takeIf { it.form == ListingForm.NEW }?.let { change ->
        ListingCategoryChangeDialog(change, viewModel::confirmCategoryChange, viewModel::cancelCategoryChange)
    }
}

@Composable
private fun CreateListingStepContent(
    step: CreateListingStep,
    state: ListingsUiState,
    viewModel: ListingsViewModel,
    pickListingImages: () -> Unit
) {
    when (step) {
        CreateListingStep.PHOTOS -> CreateListingPhotosStep(
            state = state,
            actions = CreateListingPhotosActions(
                onPickPhotos = pickListingImages,
                onRemovePhoto = viewModel::removeNewImage,
                onCropPhoto = viewModel::updateNewImageCrop,
                onTitleChange = viewModel::onTitleChanged,
                onCategorySelected = viewModel::onCategorySelected
            )
        )
        CreateListingStep.DETAILS -> CreateListingDetailsStep(
            state = state,
            actions = CreateListingDetailsActions(
                onConditionSelected = viewModel::onConditionSelected,
                onAttributeChange = viewModel::onNewAttributeChanged
            )
        )
        CreateListingStep.PRICE -> CreateListingPriceStep(
            state = state,
            actions = CreateListingPriceActions(
                onPriceChange = viewModel::onPriceChanged,
                onDescriptionChange = viewModel::onDescriptionChanged
            )
        )
    }
}
