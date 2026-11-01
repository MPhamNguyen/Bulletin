package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

/** Minimum control height; full-width primary actions use [Large]. */
enum class BulletinButtonSize { Regular, Large }

/** Shared button with centralized shape, minimum height, colors, and borders. */
@Composable
fun BulletinButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: BulletinButtonVariant = BulletinButtonVariant.Primary,
    size: BulletinButtonSize = BulletinButtonSize.Regular,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val minHeight = when (size) {
        BulletinButtonSize.Regular -> BulletinSpacing.control
        BulletinButtonSize.Large -> BulletinSpacing.controlLarge
    }
    val sized = modifier.heightIn(min = minHeight)
    val shape = BulletinButtonDefaults.shape
    val colors = BulletinButtonDefaults.colors(variant)
    val border = BulletinButtonDefaults.border(variant, enabled)
    when (variant) {
        BulletinButtonVariant.Outlined, BulletinButtonVariant.DestructiveOutlined -> OutlinedButton(
            onClick = onClick,
            modifier = sized,
            enabled = enabled,
            shape = shape,
            colors = colors,
            border = border,
            contentPadding = contentPadding,
            content = content
        )
        BulletinButtonVariant.Text, BulletinButtonVariant.DestructiveText -> TextButton(
            onClick = onClick,
            modifier = sized,
            enabled = enabled,
            shape = shape,
            colors = colors,
            contentPadding = contentPadding,
            content = content
        )
        else -> Button(
            onClick = onClick,
            modifier = sized,
            enabled = enabled,
            shape = shape,
            colors = colors,
            contentPadding = contentPadding,
            content = content
        )
    }
}
