package com.jdrms.bulletin.core.designsystem.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Emphasis levels for shared buttons. */
enum class BulletinButtonVariant {
    Primary, Outlined, Text, Neutral, Tonal, Destructive, DestructiveOutlined, DestructiveText
}

object BulletinButtonDefaults {
    val shape: Shape
        @Composable get() = MaterialTheme.shapes.medium

    private val borderWidth = 1.dp

    @Composable
    fun colors(variant: BulletinButtonVariant): ButtonColors {
        val scheme = MaterialTheme.colorScheme
        return when (variant) {
            BulletinButtonVariant.Primary -> ButtonDefaults.buttonColors(
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary
            )
            BulletinButtonVariant.Outlined -> ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = scheme.primary
            )
            BulletinButtonVariant.Text -> ButtonDefaults.textButtonColors(
                contentColor = scheme.primary
            )
            BulletinButtonVariant.Neutral -> ButtonDefaults.buttonColors(
                containerColor = scheme.surfaceVariant,
                contentColor = scheme.onSurface
            )
            BulletinButtonVariant.Tonal -> ButtonDefaults.filledTonalButtonColors(
                containerColor = scheme.primaryContainer,
                contentColor = scheme.onPrimaryContainer
            )
            BulletinButtonVariant.Destructive -> ButtonDefaults.buttonColors(
                containerColor = scheme.error,
                contentColor = scheme.onError
            )
            BulletinButtonVariant.DestructiveOutlined -> ButtonDefaults.outlinedButtonColors(
                containerColor = scheme.error.copy(alpha = 0.08f),
                contentColor = scheme.error
            )
            BulletinButtonVariant.DestructiveText -> ButtonDefaults.textButtonColors(
                contentColor = scheme.error
            )
        }
    }

    @Composable
    fun border(variant: BulletinButtonVariant, enabled: Boolean = true): BorderStroke? {
        val scheme = MaterialTheme.colorScheme
        val color = when (variant) {
            BulletinButtonVariant.Outlined -> scheme.outlineVariant
            BulletinButtonVariant.DestructiveOutlined -> scheme.error.copy(alpha = 0.45f)
            else -> return null
        }
        return BorderStroke(borderWidth, if (enabled) color else color.copy(alpha = color.alpha * DISABLED_BORDER))
    }

    private const val DISABLED_BORDER = 0.5f
}
