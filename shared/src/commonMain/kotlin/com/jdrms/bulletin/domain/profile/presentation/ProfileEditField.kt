package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinTextField
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults

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
        BulletinTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            configure = { field ->
                field.placeholder = { Text(placeholder) }
                field.leadingIcon = { Icon(leadingIcon, contentDescription = null) }
                field.trailingIcon = trailingAction
                field.singleLine = true
                field.keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
                field.shape = MaterialTheme.shapes.medium
                field.colors = BulletinTextFieldDefaults.colors()
            }
        )
    }
}
