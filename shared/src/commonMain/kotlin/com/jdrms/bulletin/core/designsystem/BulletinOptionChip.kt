package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import com.jdrms.bulletin.core.designsystem.theme.BulletinTextStyles

/** An outlined option pill, the same look as the marketplace filter sheet's option chips. */
@Composable
fun BulletinOptionChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, style = BulletinTextStyles.chip, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier.height(44.dp),
        shape = BulletinShapes.inset,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = scheme.onSurface,
            selectedContainerColor = scheme.primary,
            selectedLabelColor = scheme.onPrimary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = scheme.outline,
            selectedBorderColor = scheme.primary
        )
    )
}
