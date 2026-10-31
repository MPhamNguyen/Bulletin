package com.jdrms.bulletin.core.designsystem

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

private const val DATE_FORMAT_HINT = "YYYY-MM-DD"

/** Editable ISO date; validation and calendar selection are supplied by the caller. */
@Composable
fun DateInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null
) {
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    LaunchedEffect(value) {
        if (field.text != value) field = TextFieldValue(value, TextRange(value.length))
    }
    OutlinedTextField(
        value = field,
        onValueChange = { input ->
            val digitsBeforeCursor = dateDigits(input.text.take(input.selection.end)).length
            val formatted = formatDateDigits(dateDigits(input.text))
            field = TextFieldValue(formatted, TextRange(formattedCursorOffset(digitsBeforeCursor)))
            if (formatted != value) onValueChange(formatted)
        },
        label = { Text(label) },
        placeholder = { Text(DATE_FORMAT_HINT) },
        supportingText = { Text(error ?: "Format: $DATE_FORMAT_HINT") },
        trailingIcon = {
            IconButton(onClick = onOpenCalendar) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = "Choose date from calendar")
            }
        },
        isError = error != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = BulletinTextFieldDefaults.colors(),
        shape = MaterialTheme.shapes.small,
        modifier = modifier.showKeyboardOnFocusOrTap()
    )
}
