package com.jdrms.bulletin.domain.listings.presentation

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
internal actual fun rememberListingImagePicker(
    onImagesSelected: (List<ListingImageUpload>) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnImagesSelected = rememberUpdatedState(onImagesSelected)
    val currentOnError = rememberUpdatedState(onError)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(ListingImageUpload.MAX_LISTING_IMAGE_COUNT)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) { uris.map(context::readListingImage) }
                }.onSuccess(currentOnImagesSelected.value)
                    .onFailure { error -> currentOnError.value(error.message ?: "Unable to read selected images.") }
            }
        }
    }
    return {
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

private fun Context.readListingImage(uri: Uri): ListingImageUpload {
    val mediaType = contentResolver.getType(uri).orEmpty()
    require(mediaType in ListingImageUpload.SUPPORTED_LISTING_IMAGE_TYPES) {
        "Choose JPEG, PNG, or WebP images."
    }
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    val input = contentResolver.openInputStream(uri) ?: error("Unable to open a selected image.")
    input.use { stream ->
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            total += read
            require(total <= ListingImageUpload.MAX_LISTING_IMAGE_SIZE_BYTES) {
                "Each listing image must be 5 MB or smaller."
            }
            output.write(buffer, 0, read)
        }
    }
    return ListingImageUpload(output.toByteArray(), mediaType)
}
