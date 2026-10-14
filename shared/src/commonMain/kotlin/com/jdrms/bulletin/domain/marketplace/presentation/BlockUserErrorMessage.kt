package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
internal fun BlockUserErrorMessage(message: String?) {
    message?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
}
