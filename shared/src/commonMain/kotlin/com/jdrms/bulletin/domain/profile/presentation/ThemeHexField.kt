package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.jdrms.bulletin.core.designsystem.BulletinFieldDecoration
import com.jdrms.bulletin.core.designsystem.BulletinFieldOptions
import com.jdrms.bulletin.core.designsystem.BulletinTextField
import kotlin.math.roundToInt

@Composable
internal fun ThemeHexField(value: String, onValueChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    BulletinTextField(
        value = value,
        onValueChange = { input ->
            val digits = input.removePrefix("#")
            if (digits.length <= HEX_DIGIT_COUNT && digits.all(Char::isHexDigitValue)) {
                onValueChange(digits.lowercase())
            }
        },
        modifier = Modifier.fillMaxWidth(),
        decoration = BulletinFieldDecoration(leadingIcon = { Text("#") }),
        options = BulletinFieldOptions(
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                keyboardController?.hide()
            })
        )
    )
}

internal fun Color.toHexRgbDigits(): String = buildString(HEX_DIGIT_COUNT) {
    append(red.toByteHex())
    append(green.toByteHex())
    append(blue.toByteHex())
}

internal fun Color.withHexRgb(value: String): Color? {
    if (value.length != HEX_DIGIT_COUNT || !value.all(Char::isHexDigitValue)) return null
    val rgb = value.toLongOrNull(16) ?: return null
    val red = ((rgb shr 16) and BYTE_MASK).toInt() / MAX_BYTE_FLOAT
    val green = ((rgb shr 8) and BYTE_MASK).toInt() / MAX_BYTE_FLOAT
    val blue = (rgb and BYTE_MASK).toInt() / MAX_BYTE_FLOAT
    return Color(red, green, blue, alpha, colorSpace)
}

private fun Float.toByteHex(): String =
    (this * MAX_BYTE_FLOAT).roundToInt().toString(16).padStart(2, '0')

private fun Char.isHexDigitValue(): Boolean =
    this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

private const val HEX_DIGIT_COUNT = 6
private const val BYTE_MASK = 0xFFL
private const val MAX_BYTE_FLOAT = 255f
