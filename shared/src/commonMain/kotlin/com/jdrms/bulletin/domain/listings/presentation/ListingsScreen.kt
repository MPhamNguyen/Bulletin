package com.jdrms.bulletin.domain.listings.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

@Composable
fun ListingsScreen(viewModel: ListingsViewModel, onClose: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsState()
    val pickListingImages = rememberListingImagePicker(viewModel::addNewImages, viewModel::imageSelectionFailed)
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val step = CreateListingStep.entries[stepIndex]
    val focusManager = LocalFocusManager.current

    BackHandler { if (step.isFirst) onClose() else stepIndex-- }
    // Drop focus from the outgoing step's field so the keyboard doesn't follow the slide.
    LaunchedEffect(stepIndex) { focusManager.clearFocus() }
    // A rejected attribute value lives on the details step, so take the seller there.
    LaunchedEffect(state.newAttributes.errors) {
        if (state.newAttributes.errors.isNotEmpty()) stepIndex = CreateListingStep.DETAILS.ordinal
    }
    // The view model clears the title only after a successful post; close the flow as the X button does.
    val wasSubmitting = remember { booleanArrayOf(false) }
    LaunchedEffect(state.isSubmitting) {
        if (wasSubmitting[0] && !state.isSubmitting && state.newTitle.isEmpty()) {
            stepIndex = 0
            onClose()
        }
        wasSubmitting[0] = state.isSubmitting
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            CreateListingHeader(step, onClose)
            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f),
                transitionSpec = { createListingStepTransition(initialState, targetState) },
                label = "createListingStep"
            ) { animatedStep ->
                // Each step owns its scroll state so the outgoing step doesn't jump to the top mid-slide.
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    CreateListingStepContent(animatedStep, state, viewModel, pickListingImages)
                }
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
                modifier = Modifier.align(
                    Alignment.TopCenter
                ).padding(
                    top = 72.dp,
                    start = BulletinSpacing.xl,
                    end = BulletinSpacing.xl
                )
            )
        }
    }

    state.pendingCategoryChange?.takeIf { it.form == ListingForm.NEW }?.let { change ->
        ListingCategoryChangeDialog(change, viewModel::confirmCategoryChange, viewModel::cancelCategoryChange)
    }
}

private const val STEP_TRANSITION_MILLIS = 380
private const val STEP_TRAVEL_DIVISOR = 3

/** +1 when moving to a later step (slide in from the right), -1 when moving back (slide in from the left). */
internal fun stepDirection(from: CreateListingStep, to: CreateListingStep): Int =
    if (to.ordinal > from.ordinal) 1 else -1

private fun createListingStepTransition(from: CreateListingStep, to: CreateListingStep): ContentTransform {
    val direction = stepDirection(from, to)
    val fade = tween<Float>(STEP_TRANSITION_MILLIS)
    val slide = tween<IntOffset>(STEP_TRANSITION_MILLIS)
    val enter = slideInHorizontally(slide) { width -> direction * width / STEP_TRAVEL_DIVISOR } + fadeIn(fade)
    val exit = slideOutHorizontally(slide) { width -> -direction * width / STEP_TRAVEL_DIVISOR } + fadeOut(fade)
    return enter.togetherWith(exit)
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
