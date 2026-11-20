package com.jdrms.bulletin.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Subtle neutral containers for inline status summaries. */
object BulletinStatusDefaults {
    val containerColor: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)

    val outlineColor: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
}
