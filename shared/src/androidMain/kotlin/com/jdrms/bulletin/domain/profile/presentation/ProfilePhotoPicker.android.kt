package com.jdrms.bulletin.domain.profile.presentation

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.jdrms.bulletin.domain.profile.domain.model.ProfilePhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
internal actual fun rememberProfilePhotoPicker(
    onPhotoSelected: (ByteArray, String) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnPhotoSelected = rememberUpdatedState(onPhotoSelected)
    val currentOnError = rememberUpdatedState(onError)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) { context.readProfilePhoto(uri) }
                }.onSuccess { selection ->
                    currentOnPhotoSelected.value(selection.bytes, selection.mediaType)
                }.onFailure { error ->
                    val message = if (error is ProfilePhotoTooLargeException) {
                        "Profile photos must be 5 MB or smaller."
                    } else {
                        "Unable to read the selected image."
                    }
                    currentOnError.value(message)
                }
            }
        }
    }
    return { launcher.launch("image/*") }
}

private data class SelectedProfilePhoto(
    val bytes: ByteArray,
    val mediaType: String
)

private class ProfilePhotoTooLargeException : IllegalArgumentException()

private fun Context.readProfilePhoto(uri: Uri): SelectedProfilePhoto {
    val mediaType = contentResolver.getType(uri).orEmpty()
    val bytes = contentResolver.openInputStream(uri)?.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > ProfilePhoto.MAX_SIZE_BYTES) {
                throw ProfilePhotoTooLargeException()
            }
            output.write(buffer, 0, read)
        }
        output.toByteArray()
    } ?: error("The selected image could not be opened.")
    return SelectedProfilePhoto(bytes = bytes, mediaType = mediaType)
}
