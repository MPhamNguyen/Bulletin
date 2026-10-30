package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
internal fun ListingRentAttributeField(
    definition: ListingAttributeDefinition,
    value: String,
    error: String?,
    onInputChange: (String, String) -> Unit
) {
    val text = formatCurrencyDigits(rentAmountToDigits(value))
    var fieldValue by remember(text) {
        mutableStateOf(TextFieldValue(text, TextRange(text.length)))
    }
    OutlinedTextField(
        value = fieldValue,
        onValueChange = {
            val digits = normalizeCurrencyTextChange(it, fieldValue)
            onInputChange(definition.key, rentDigitsToAmount(digits))
            val nextText = formatCurrencyDigits(digits)
            fieldValue = TextFieldValue(nextText, TextRange(nextText.length))
        },
        label = { Text(ListingAttributeDisplay.fieldLabel(definition)) },
        placeholder = { Text("$0.00") },
        supportingText = if (error != null) {
            { Text(error) }
        } else {
            null
        },
        isError = error != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = BulletinTextFieldDefaults.colors(),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    )
}
