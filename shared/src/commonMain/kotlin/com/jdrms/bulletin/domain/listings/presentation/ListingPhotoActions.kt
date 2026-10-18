package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class ListingPhotoActions(private val state: MutableStateFlow<ListingsUiState>) {
    fun addNew(images: List<ListingImageUpload>) = state.update { current ->
        val selected = current.newImages + images
        if (selected.size > ListingImageUpload.MAX_LISTING_IMAGE_COUNT) {
            current.copy(errorMessage = "A listing can have up to 5 images.")
        } else {
            current.copy(newImages = selected, errorMessage = null)
        }
    }

    fun removeNew(index: Int) = state.update { current ->
        current.copy(newImages = current.newImages.filterIndexed { position, _ -> position != index })
    }

    fun addEdit(images: List<ListingImageUpload>) = state.update { current ->
        val selected = current.editPhotos + images.map { ListingPhotoDraft.Pending(it) }
        if (selected.size > ListingImageUpload.MAX_LISTING_IMAGE_COUNT) {
            current.copy(errorMessage = "A listing can have up to 5 images.")
        } else {
            current.copy(editPhotos = selected, errorMessage = null)
        }
    }

    fun removeEdit(index: Int) = state.update { current ->
        current.copy(editPhotos = current.editPhotos.filterIndexed { position, _ -> position != index })
    }

    fun failed(message: String) = state.update { it.copy(errorMessage = message) }
}
