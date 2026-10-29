package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults
import com.jdrms.bulletin.core.designsystem.showKeyboardOnFocusOrTap

@Composable
internal fun BioField(value: String, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        FieldLabel("Bio")
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.take(BIO_MAX_LENGTH)) },
            placeholder = { Text("Introduce yourself to buyers and sellers on campus…") },
            minLines = 3,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Default
            ),
            shape = MaterialTheme.shapes.medium,
            colors = BulletinTextFieldDefaults.colors(),
            modifier = Modifier.showKeyboardOnFocusOrTap().fillMaxWidth()
        )
        BioFieldFooter(value)
    }
}

@Composable
internal expect fun rememberProfilePhotoPicker(
    onPhotoSelected: (ByteArray, String) -> Unit,
    onError: (String) -> Unit
): () -> Unit

@Composable
private fun BioFieldFooter(value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Shown on your listings and public profile.",
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "${value.length} / $BIO_MAX_LENGTH",
            style = MaterialTheme.typography.labelMedium,
            color = if (value.length >= BIO_WARN_LENGTH) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
