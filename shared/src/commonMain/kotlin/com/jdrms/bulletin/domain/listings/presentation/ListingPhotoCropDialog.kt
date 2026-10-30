package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jdrms.bulletin.core.common.ImageCropSettings

@Composable
internal fun ListingPhotoCropDialog(
    photo: ListingPhotoDraft,
    initialCrop: ImageCropSettings,
    onDismiss: () -> Unit,
    onSave: (ImageCropSettings) -> Unit
) {
    val cropState = remember(photo) { mutableStateOf(initialCrop) }
    val crop by cropState
    var frameSize by remember { mutableStateOf(1f) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.widthIn(max = 360.dp).clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Adjust photo", style = MaterialTheme.typography.titleMedium)
            ListingPhotoImage(
                photo = photo,
                crop = crop,
                description = "Photo crop preview",
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.medium)
                    .onSizeChanged { frameSize = it.width.toFloat().coerceAtLeast(1f) }
                    .pointerInput(photo, frameSize) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val current = cropState.value
                            val scale = (current.scale * zoom).coerceIn(1f, 4f)
                            val maxOffset = ((scale - 1f) / (2f * scale)).coerceAtLeast(0f)
                            cropState.value = ImageCropSettings(
                                scale = scale,
                                offsetX = (current.offsetX - pan.x / frameSize / current.scale)
                                    .coerceIn(-maxOffset, maxOffset),
                                offsetY = (current.offsetY - pan.y / frameSize / current.scale)
                                    .coerceIn(-maxOffset, maxOffset)
                            )
                        }
                    }
            )
            Text(
                "Pinch to zoom and drag to reposition",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = { onSave(crop) }, modifier = Modifier.weight(1f)) { Text("Save crop") }
            }
        }
    }
}
