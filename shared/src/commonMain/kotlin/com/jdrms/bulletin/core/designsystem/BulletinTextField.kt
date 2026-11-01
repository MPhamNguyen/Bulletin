package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import com.jdrms.bulletin.core.designsystem.theme.BulletinTextFieldDefaults

/** Optional slots drawn around the input text. */
class BulletinFieldDecoration(
    val label: (@Composable () -> Unit)? = null,
    val placeholder: (@Composable () -> Unit)? = null,
    val supportingText: (@Composable () -> Unit)? = null,
    val leadingIcon: (@Composable () -> Unit)? = null,
    val trailingIcon: (@Composable () -> Unit)? = null
)

/** Input behavior. Set [maxLines] above one for multiline fields. */
class BulletinFieldOptions(
    val enabled: Boolean = true,
    val readOnly: Boolean = false,
    val minLines: Int = 1,
    val maxLines: Int = 1,
    val textStyle: TextStyle? = null,
    val keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    val keyboardActions: KeyboardActions = KeyboardActions.Default,
    val visualTransformation: VisualTransformation = VisualTransformation.None
)

/** Shared input with design-system styling and keyboard-on-tap behavior. */
@Composable
fun BulletinTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    decoration: BulletinFieldDecoration = BulletinFieldDecoration(),
    options: BulletinFieldOptions = BulletinFieldOptions()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.showKeyboardOnFocusOrTap(options.enabled, options.readOnly),
        enabled = options.enabled,
        readOnly = options.readOnly,
        textStyle = options.textStyle ?: LocalTextStyle.current,
        label = decoration.label,
        placeholder = decoration.placeholder,
        leadingIcon = decoration.leadingIcon,
        trailingIcon = decoration.trailingIcon,
        supportingText = decoration.supportingText,
        isError = isError,
        visualTransformation = options.visualTransformation,
        keyboardOptions = options.keyboardOptions,
        keyboardActions = options.keyboardActions,
        singleLine = options.maxLines == 1,
        maxLines = options.maxLines,
        minLines = options.minLines,
        shape = BulletinTextFieldDefaults.shape,
        colors = BulletinTextFieldDefaults.colors()
    )
}

/** Shared input with design-system styling and keyboard-on-tap behavior. */
@Composable
fun BulletinTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    decoration: BulletinFieldDecoration = BulletinFieldDecoration(),
    options: BulletinFieldOptions = BulletinFieldOptions()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.showKeyboardOnFocusOrTap(options.enabled, options.readOnly),
        enabled = options.enabled,
        readOnly = options.readOnly,
        textStyle = options.textStyle ?: LocalTextStyle.current,
        label = decoration.label,
        placeholder = decoration.placeholder,
        leadingIcon = decoration.leadingIcon,
        trailingIcon = decoration.trailingIcon,
        supportingText = decoration.supportingText,
        isError = isError,
        visualTransformation = options.visualTransformation,
        keyboardOptions = options.keyboardOptions,
        keyboardActions = options.keyboardActions,
        singleLine = options.maxLines == 1,
        maxLines = options.maxLines,
        minLines = options.minLines,
        shape = BulletinTextFieldDefaults.shape,
        colors = BulletinTextFieldDefaults.colors()
    )
}
