package com.jdrms.bulletin.core.designsystem.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp

/** Equal geometry with distinct available, selected, and unavailable option states. */
object BulletinOptionChipDefaults {
    val disabledLabelColor: Color
        @Composable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)

    @Composable
    fun colors(): SelectableChipColors {
        val scheme = MaterialTheme.colorScheme
        return FilterChipDefaults.filterChipColors(
            containerColor = scheme.surfaceContainerHigh,
            labelColor = scheme.onSurface,
            iconColor = scheme.onSurface,
            selectedContainerColor = scheme.primary.copy(alpha = SELECTED_TINT_ALPHA)
                .compositeOver(scheme.surfaceContainerHigh),
            selectedLabelColor = scheme.onSurface,
            selectedLeadingIconColor = scheme.primary,
            disabledContainerColor = scheme.surfaceContainerLow,
            disabledSelectedContainerColor = scheme.surfaceContainerLow,
            disabledLabelColor = disabledLabelColor
        )
    }

    @Composable
    fun border(enabled: Boolean, selected: Boolean): BorderStroke {
        val scheme = MaterialTheme.colorScheme
        val color = when {
            !enabled -> scheme.outlineVariant.copy(alpha = DISABLED_BORDER_ALPHA)
            selected -> scheme.primary
            else -> scheme.outline
        }
        return BorderStroke(if (selected) 2.dp else 1.dp, color)
    }

    private const val SELECTED_TINT_ALPHA = 0.14f
    private const val DISABLED_CONTENT_ALPHA = 0.38f
    private const val DISABLED_BORDER_ALPHA = 0.35f
}
