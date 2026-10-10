package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults

@Composable
internal fun CurrencyTextField(
    digits: String,
    onDigitsChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    colors: TextFieldColors = BulletinTextFieldDefaults.colors(),
    shape: Shape = MaterialTheme.shapes.small
) {
    val text = formatCurrencyDigits(digits)
    var fieldValue by remember(text) {
        mutableStateOf(TextFieldValue(text = text, selection = TextRange(text.length)))
    }
    OutlinedTextField(
        value = fieldValue,
        onValueChange = {
            val nextDigits = normalizeCurrencyTextChange(it, fieldValue)
            onDigitsChange(nextDigits)
            val nextText = formatCurrencyDigits(nextDigits)
            fieldValue = TextFieldValue(text = nextText, selection = TextRange(nextText.length))
        },
        label = { Text("Price (USD)") },
        placeholder = { Text("$0.00") },
        supportingText = { Text("Enter cents from right to left (e.g. 1250 = $12.50)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = colors,
        shape = shape,
        modifier = modifier
    )
}
