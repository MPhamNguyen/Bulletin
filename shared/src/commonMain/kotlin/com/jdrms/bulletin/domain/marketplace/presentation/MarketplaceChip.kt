package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import com.jdrms.bulletin.core.designsystem.theme.BulletinTextStyles

/** The chip families on the marketplace screen; each has its own height, radius, and fill. */
internal enum class MarketplaceChipStyle(val height: Dp, val iconSize: Dp) {
    /** Category row and sort control: 40dp, fully rounded, outlined. */
    Category(40.dp, 16.dp),

    /** Option pills inside the filter sheet: 44dp, 14dp radius, outlined. */
    Option(44.dp, 16.dp),

    /** Removable summary of an applied filter: 34dp, fully rounded, tinted fill. */
    Active(34.dp, 14.dp)
}

@Composable
internal fun MarketplaceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: MarketplaceChipStyle = MarketplaceChipStyle.Category,
    trailingIcon: ImageVector? = null,
    description: String? = null
) {
    val scheme = MaterialTheme.colorScheme
    val isActive = style == MarketplaceChipStyle.Active
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = if (isActive) BulletinTextStyles.chipCompact else BulletinTextStyles.chip,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier
            .height(style.height)
            .semantics { description?.let { contentDescription = it } },
        trailingIcon = trailingIcon?.let { icon ->
            {
                Icon(icon, contentDescription = null, modifier = Modifier.size(style.iconSize))
            }
        },
        shape = style.shape(),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = if (isActive) scheme.surfaceContainerHighest else Color.Transparent,
            labelColor = scheme.onSurface,
            iconColor = scheme.onSurface,
            selectedContainerColor = scheme.primary,
            selectedLabelColor = scheme.onPrimary,
            selectedTrailingIconColor = scheme.onPrimary
        ),
        border = if (isActive) {
            null
        } else {
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor = scheme.outline,
                selectedBorderColor = scheme.primary
            )
        }
    )
}

private fun MarketplaceChipStyle.shape(): Shape =
    if (this == MarketplaceChipStyle.Option) BulletinShapes.inset else BulletinShapes.pill
