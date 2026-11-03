package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle

@Composable
internal fun ListingRequiredLabel(
    label: String,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    Row {
        Text(label, style = style)
        Text(
            text = " *",
            style = style,
            color = MaterialTheme.colorScheme.error
        )
    }
}
