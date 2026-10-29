package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation

/** Per-field extensions to the shared input. Focus dismissal always stays with the input host. */
class BulletinTextFieldOptions {
    var enabled: Boolean = true
    var readOnly: Boolean = false
    var singleLine: Boolean = false
    var minLines: Int = 1
    var maxLines: Int = Int.MAX_VALUE
    var isError: Boolean = false
    var textStyle: TextStyle? = null
    var shape: Shape? = null
    var colors: TextFieldColors? = null
    var visualTransformation: VisualTransformation = VisualTransformation.None
    var keyboardOptions: KeyboardOptions = KeyboardOptions.Default
    var keyboardActions: KeyboardActions? = null
    var label: (@Composable () -> Unit)? = null
    var placeholder: (@Composable () -> Unit)? = null
    var leadingIcon: (@Composable () -> Unit)? = null
    var trailingIcon: (@Composable () -> Unit)? = null
    var supportingText: (@Composable () -> Unit)? = null

    internal fun resolvedKeyboardOptions(): KeyboardOptions {
        val action = keyboardOptions.imeAction
        return if (singleLine && (action == ImeAction.Default || action == ImeAction.Unspecified)) {
            keyboardOptions.copy(imeAction = ImeAction.Done)
        } else {
            keyboardOptions
        }
    }
}
