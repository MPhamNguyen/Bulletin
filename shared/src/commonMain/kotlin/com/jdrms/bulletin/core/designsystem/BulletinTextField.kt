package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.TextFieldValue

/** Shared input foundation; callers customize presentation and keyboard behavior through [configure]. */
@Composable
fun BulletinTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    configure: @Composable (BulletinTextFieldOptions) -> Unit = {}
) {
    val options = BulletinTextFieldOptions().also { configure(it) }
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.bulletinInputFocus(),
        enabled = options.enabled,
        readOnly = options.readOnly,
        textStyle = options.textStyle ?: LocalTextStyle.current,
        label = options.label,
        placeholder = options.placeholder,
        leadingIcon = options.leadingIcon,
        trailingIcon = options.trailingIcon,
        supportingText = options.supportingText,
        isError = options.isError,
        visualTransformation = options.visualTransformation,
        keyboardOptions = options.resolvedKeyboardOptions(),
        keyboardActions = options.keyboardActions ?: KeyboardActions(onDone = { focus.clearFocus() }),
        singleLine = options.singleLine,
        minLines = options.minLines,
        maxLines = if (options.singleLine) 1 else options.maxLines,
        shape = options.shape ?: MaterialTheme.shapes.small,
        colors = options.colors ?: BulletinTextFieldDefaults.colors()
    )
}

/** Shared input foundation; callers customize presentation and keyboard behavior through [configure]. */
@Composable
fun BulletinTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    configure: @Composable (BulletinTextFieldOptions) -> Unit = {}
) {
    val options = BulletinTextFieldOptions().also { configure(it) }
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.bulletinInputFocus(),
        enabled = options.enabled,
        readOnly = options.readOnly,
        textStyle = options.textStyle ?: LocalTextStyle.current,
        label = options.label,
        placeholder = options.placeholder,
        leadingIcon = options.leadingIcon,
        trailingIcon = options.trailingIcon,
        supportingText = options.supportingText,
        isError = options.isError,
        visualTransformation = options.visualTransformation,
        keyboardOptions = options.resolvedKeyboardOptions(),
        keyboardActions = options.keyboardActions ?: KeyboardActions(onDone = { focus.clearFocus() }),
        singleLine = options.singleLine,
        minLines = options.minLines,
        maxLines = if (options.singleLine) 1 else options.maxLines,
        shape = options.shape ?: MaterialTheme.shapes.small,
        colors = options.colors ?: BulletinTextFieldDefaults.colors()
    )
}
