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
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinOptionChip
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDefinition
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeType
import com.jdrms.bulletin.domain.listings.domain.model.ListingStorageOptions

/** Single-select storage sizes; choosing another size replaces the current one and tapping it again clears it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ListingStorageAttributeField(
    definition: ListingAttributeDefinition,
    value: String,
    error: String?,
    onInputChange: (String, String) -> Unit
) {
    val sizes = (definition.type as ListingAttributeType.Integer).allowedValues.sorted()
    val selected = value.toLongOrNull()
    val summary = selected?.let(ListingStorageOptions::label) ?: "Not selected"
    Column(modifier = Modifier.semantics { stateDescription = summary }) {
        Text(
            text = definition.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sizes.forEach { size ->
                BulletinOptionChip(
                    label = ListingStorageOptions.label(size),
                    selected = size == selected,
                    onClick = { onInputChange(definition.key, if (size == selected) "" else size.toString()) }
                )
            }
        }
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}
