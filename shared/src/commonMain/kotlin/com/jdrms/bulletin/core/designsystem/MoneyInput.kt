package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun MoneyInput(
    digits: String,
    onDigitsChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
    shape: Shape = MaterialTheme.shapes.small,
    textStyle: TextStyle = LocalTextStyle.current,
    error: String? = null
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
        label = label,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        placeholder = { Text("$0.00") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = textStyle,
        colors = BulletinTextFieldDefaults.colors(),
        shape = shape,
        modifier = modifier.showKeyboardOnFocusOrTap()
    )
}
