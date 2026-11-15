package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

/** A titled block in the filter sheet with an optional one-line hint. */
@Composable
internal fun MarketplaceFilterSection(
    title: String,
    hint: String? = null,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)) {
        Column(verticalArrangement = Arrangement.spacedBy(BulletinSpacing.xs)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            hint?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        content()
    }
}

/** Wrapping group of option pills; tapping a selected pill clears it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> MarketplacePillGroup(
    options: List<T>,
    isSelected: (T) -> Boolean,
    label: (T) -> String,
    onToggle: (T) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s),
        verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
    ) {
        options.forEach { option ->
            MarketplaceChip(
                label = label(option),
                selected = isSelected(option),
                onClick = { onToggle(option) },
                style = MarketplaceChipStyle.Option
            )
        }
    }
}

/** Rule plus heading that separates the common filters from the category's own. */
@Composable
internal fun MarketplaceFilterDivider(title: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
