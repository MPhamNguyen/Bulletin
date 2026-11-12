package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.jdrms.bulletin.core.common.Result

/** When [onInputError] is supplied, the caller renders input errors; null reports a valid edit. */
@Composable
fun MoneyInput(
    digits: String,
    onDigitsChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    textStyle: TextStyle = LocalTextStyle.current,
    error: String? = null,
    appearance: MoneyInputAppearance = MoneyInputAppearance(),
    onInputError: ((String?) -> Unit)? = null
) {
    val text = formatCurrencyDigits(digits).let { formatted ->
        if (appearance.currencySymbolAsAdornment) formatted.removePrefix("$") else formatted
    }
    var fieldValue by remember(text) {
        mutableStateOf(TextFieldValue(text = text, selection = TextRange(text.length)))
    }
    var inputError by remember(text) { mutableStateOf<String?>(null) }
    val displayedError = inputError ?: error
    BulletinTextField(
        value = fieldValue,
        onValueChange = {
            if (it.text == fieldValue.text) {
                fieldValue = it
            } else {
                when (val result = normalizeCurrencyTextChange(it, fieldValue)) {
                    is Result.Success -> {
                        inputError = null
                        onInputError?.invoke(null)
                        onDigitsChange(result.data)
                        val nextText = formatCurrencyDigits(result.data).let { formatted ->
                            if (appearance.currencySymbolAsAdornment) formatted.removePrefix("$") else formatted
                        }
                        fieldValue = TextFieldValue(text = nextText, selection = TextRange(nextText.length))
                    }
                    is Result.Error -> {
                        inputError = result.message
                        onInputError?.invoke(result.message)
                    }
                }
            }
        },
        modifier = modifier,
        isError = displayedError != null,
        decoration = BulletinFieldDecoration(
            label = label,
            placeholder = { Text(appearance.placeholder ?: if (appearance.currencySymbolAsAdornment) "0.00" else "$0.00") },
            leadingIcon = if (appearance.currencySymbolAsAdornment) ({ Text("$") }) else null,
            supportingText = if (onInputError == null) displayedError?.let { { Text(it) } } else null
        ),
        options = BulletinFieldOptions(
            textStyle = textStyle,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        ),
        style = appearance.style
    )
}
