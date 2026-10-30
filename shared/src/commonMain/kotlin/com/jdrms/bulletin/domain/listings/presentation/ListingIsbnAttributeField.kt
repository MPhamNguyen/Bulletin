package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDefinition

private const val ISBN_FORMAT_HINT = "ISBN-10: 10 digits or 9 digits + X · ISBN-13: 978/979 + 10 digits"

@Composable
internal fun ListingIsbnAttributeField(
    definition: ListingAttributeDefinition,
    value: String,
    error: String?,
    onInputChange: (String, String) -> Unit
) {
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    LaunchedEffect(value) {
        if (field.text != value) field = TextFieldValue(value, TextRange(value.length))
    }
    val formatError = isbnInputError(field.text)
    val message = error ?: formatError
    OutlinedTextField(
        value = field,
        onValueChange = { input ->
            val formatted = formatIsbnInput(input.text)
            val cursor = formattedIsbnCursorOffset(input.text.take(input.selection.end))
            field = TextFieldValue(formatted, TextRange(cursor))
            if (formatted != value) onInputChange(definition.key, formatted)
        },
        label = { Text(ListingAttributeDisplay.fieldLabel(definition)) },
        supportingText = { Text(message ?: ISBN_FORMAT_HINT) },
        isError = message != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        colors = BulletinTextFieldDefaults.colors(),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    )
}
