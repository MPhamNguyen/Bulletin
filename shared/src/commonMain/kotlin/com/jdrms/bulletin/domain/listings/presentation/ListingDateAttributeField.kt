package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeType
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputParser

private const val DATE_FORMAT_HINT = "YYYY-MM-DD"

@Composable
internal fun ListingDateAttributeField(
    category: ListingCategory,
    definition: ListingAttributeDefinition,
    value: String,
    error: String?,
    onInputChange: (String, String) -> Unit
) {
    val type = definition.type as ListingAttributeType.Date
    val parser = remember { ListingAttributeInputParser() }
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    LaunchedEffect(value) {
        if (field.text != value) field = TextFieldValue(value, TextRange(value.length))
    }
    val openCalendar = rememberListingDatePicker(
        initial = parseDateInput(value),
        minYear = type.minYear,
        maxYear = type.maxYear,
        onDateSelected = { onInputChange(definition.key, it.toString()) }
    )
    val message = error ?: dateInputError(parser, category, definition, value)
    OutlinedTextField(
        value = field,
        onValueChange = { input ->
            val digitsBeforeCursor = dateDigits(input.text.take(input.selection.end)).length
            val formatted = formatDateDigits(dateDigits(input.text))
            field = TextFieldValue(formatted, TextRange(formattedCursorOffset(digitsBeforeCursor)))
            if (formatted != value) onInputChange(definition.key, formatted)
        },
        label = { Text(ListingAttributeDisplay.fieldLabel(definition)) },
        placeholder = { Text(DATE_FORMAT_HINT) },
        supportingText = { Text(message ?: "Format: $DATE_FORMAT_HINT") },
        trailingIcon = {
            IconButton(onClick = openCalendar) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = "Choose date from calendar")
            }
        },
        isError = message != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = BulletinTextFieldDefaults.colors(),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    )
}
