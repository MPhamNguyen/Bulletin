package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.jdrms.bulletin.core.designsystem.BulletinOptionChip
import com.jdrms.bulletin.core.designsystem.theme.BulletinOptionChipDefaults
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDefinition
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeType
import com.jdrms.bulletin.domain.listings.domain.model.ListingCapacityOptions

/** Single-select capacity sizes; choosing another size replaces the current one and tapping it again clears it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ListingCapacityAttributeField(
    definition: ListingAttributeDefinition,
    value: String,
    error: String?,
    onInputChange: (String, String) -> Unit,
    enabled: Boolean,
    unavailableReason: String?
) {
    val sizes = (definition.type as ListingAttributeType.Integer).allowedValues.sorted()
    val selected = value.toLongOrNull()
    val capacityLabel: (Long) -> String = { size ->
        ListingCapacityOptions.label(size) + if (size == sizes.lastOrNull()) "+" else ""
    }
    val summary = selected?.let(capacityLabel) ?: "Not selected"
    Column(
        modifier = Modifier.semantics { stateDescription = summary },
        verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
    ) {
        Text(
            text = definition.label,
            style = MaterialTheme.typography.titleSmall,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                BulletinOptionChipDefaults.disabledLabelColor
            }
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s),
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
        ) {
            sizes.forEach { size ->
                BulletinOptionChip(
                    label = capacityLabel(size),
                    selected = size == selected,
                    enabled = enabled,
                    onClick = { onInputChange(definition.key, if (size == selected) "" else size.toString()) }
                )
            }
        }
        unavailableReason?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}
