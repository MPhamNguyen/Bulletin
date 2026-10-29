package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults
import com.jdrms.bulletin.core.designsystem.showKeyboardOnFocusOrTap

@Composable
internal fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium)
}

@Composable
internal fun ProfileEditField(
    label: String,
    value: String,
    leadingIcon: ImageVector,
    placeholder: String = "",
    trailingAction: (@Composable () -> Unit)? = null,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        FieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(leadingIcon, contentDescription = null) },
            trailingIcon = trailingAction,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            shape = MaterialTheme.shapes.medium,
            colors = BulletinTextFieldDefaults.colors(),
            modifier = Modifier.showKeyboardOnFocusOrTap().fillMaxWidth()
        )
    }
}
