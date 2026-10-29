package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.*

/** Bulletin styling and keyboard behavior for editable text. */
@Composable
fun BulletinTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    options: @Composable BulletinTextFieldOptions.() -> Unit = {}
) {
    val appearance = BulletinTextFieldOptions()
    appearance.options()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.showKeyboardOnFocusOrTap(appearance.enabled, appearance.readOnly),
        enabled = appearance.enabled,
        readOnly = appearance.readOnly,
        textStyle = appearance.textStyle ?: LocalTextStyle.current,
        label = appearance.label,
        placeholder = appearance.placeholder,
        leadingIcon = appearance.leadingIcon,
        trailingIcon = appearance.trailingIcon,
        supportingText = appearance.supportingText,
        isError = appearance.isError,
        visualTransformation = appearance.visualTransformation,
        keyboardOptions = keyboardOptions.withBulletinImeAction(singleLine),
        keyboardActions = keyboardActions.withDefaultDone(),
        singleLine = singleLine,
        minLines = appearance.minLines,
        maxLines = if (singleLine) 1 else appearance.maxLines,
        shape = appearance.shape ?: MaterialTheme.shapes.medium,
        colors = appearance.colors ?: BulletinTextFieldDefaults.colors()
    )
}

/** Bulletin styling and keyboard behavior for editable text. */
@Composable
fun BulletinTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    options: @Composable BulletinTextFieldOptions.() -> Unit = {}
) {
    val appearance = BulletinTextFieldOptions()
    appearance.options()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.showKeyboardOnFocusOrTap(appearance.enabled, appearance.readOnly),
        enabled = appearance.enabled,
        readOnly = appearance.readOnly,
        textStyle = appearance.textStyle ?: LocalTextStyle.current,
        label = appearance.label,
        placeholder = appearance.placeholder,
        leadingIcon = appearance.leadingIcon,
        trailingIcon = appearance.trailingIcon,
        supportingText = appearance.supportingText,
        isError = appearance.isError,
        visualTransformation = appearance.visualTransformation,
        keyboardOptions = keyboardOptions.withBulletinImeAction(singleLine),
        keyboardActions = keyboardActions.withDefaultDone(),
        singleLine = singleLine,
        minLines = appearance.minLines,
        maxLines = if (singleLine) 1 else appearance.maxLines,
        shape = appearance.shape ?: MaterialTheme.shapes.medium,
        colors = appearance.colors ?: BulletinTextFieldDefaults.colors()
    )
}

internal fun KeyboardOptions.withBulletinImeAction(singleLine: Boolean): KeyboardOptions =
    if (singleLine && (imeAction == ImeAction.Default || imeAction == ImeAction.Unspecified)) {
        copy(imeAction = ImeAction.Done)
    } else {
        this
    }

@Composable
private fun KeyboardActions.withDefaultDone(): KeyboardActions {
    if (onDone != null) return this
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    return KeyboardActions(
        onDone = {
            focusManager.clearFocus()
            keyboard?.hide()
        },
        onGo = onGo,
        onNext = onNext,
        onPrevious = onPrevious,
        onSearch = onSearch,
        onSend = onSend
    )
}
