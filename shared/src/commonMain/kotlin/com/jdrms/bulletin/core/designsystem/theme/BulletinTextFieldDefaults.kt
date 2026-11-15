package com.jdrms.bulletin.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape

/** Standard fields sit on a tinted fill; Search and Inset fields sit on the surface color with a larger radius. */
enum class BulletinFieldStyle { Standard, Search, Inset }

/** Visual tokens for every Bulletin text input. */
object BulletinTextFieldDefaults {
    val shape: Shape
        @Composable get() = shape(BulletinFieldStyle.Standard)

    @Composable
    fun shape(style: BulletinFieldStyle): Shape = when (style) {
        BulletinFieldStyle.Standard -> MaterialTheme.shapes.medium
        BulletinFieldStyle.Search -> BulletinShapes.searchField
        BulletinFieldStyle.Inset -> BulletinShapes.inset
    }

    @Composable
    fun colors(style: BulletinFieldStyle = BulletinFieldStyle.Standard): TextFieldColors {
        val scheme = MaterialTheme.colorScheme
        val container = if (style == BulletinFieldStyle.Standard) scheme.surfaceVariant else scheme.surface
        val placeholder = scheme.onSurfaceVariant
        return OutlinedTextFieldDefaults.colors(
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            disabledContainerColor = container,
            errorContainerColor = container,
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
