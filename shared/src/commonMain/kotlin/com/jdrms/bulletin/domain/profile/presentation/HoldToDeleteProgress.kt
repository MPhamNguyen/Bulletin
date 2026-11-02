package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun HoldProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier
) {
    if (progress > 0f) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                .background(MaterialTheme.colorScheme.error)
        )
    }
}

@Composable
internal fun HoldButtonLabel(
    progress: Float,
    isPressed: Boolean
) {
    val label = when {
        progress >= 1f -> "Deleting…"
        isPressed -> "Keep holding…"
        else -> "Hold to delete"
    }
    val textColor = if (progress > 0.5f) {
        MaterialTheme.colorScheme.onError
    } else {
        MaterialTheme.colorScheme.error
    }
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color = textColor
    )
}
