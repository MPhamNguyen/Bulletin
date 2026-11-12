package com.jdrms.bulletin.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape

/** Visual tokens for every Bulletin text input. */
object BulletinTextFieldDefaults {
    val shape: Shape
        @Composable get() = MaterialTheme.shapes.medium

    @Composable
    fun colors(): TextFieldColors {
        val scheme = MaterialTheme.colorScheme
        val placeholder = scheme.onSurfaceVariant
        return OutlinedTextFieldDefaults.colors(
            focusedContainerColor = scheme.surfaceVariant,
            unfocusedContainerColor = scheme.surfaceVariant,
            disabledContainerColor = scheme.surfaceVariant,
            errorContainerColor = scheme.surfaceVariant,
            focusedBorderColor = MaterialTheme.bulletinColors.ring,
            cursorColor = MaterialTheme.bulletinColors.ring,
            unfocusedBorderColor = scheme.outline,
            errorBorderColor = scheme.error,
            focusedPlaceholderColor = placeholder,
            unfocusedPlaceholderColor = placeholder,
            errorPlaceholderColor = placeholder
        )
    }
}
