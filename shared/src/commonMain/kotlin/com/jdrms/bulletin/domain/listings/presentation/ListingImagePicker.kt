package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.runtime.Composable
import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload

@Composable
internal expect fun rememberListingImagePicker(
    onImagesSelected: (List<ListingImageUpload>) -> Unit,
    onError: (String) -> Unit
): () -> Unit
