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
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDefinition
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeOption

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ListingChoiceAttributeField(
    definition: ListingAttributeDefinition,
    options: List<ListingAttributeOption>,
    selectedCode: String,
    error: String?,
    onInputChange: (String, String) -> Unit
) {
    val selectedLabel = options.firstOrNull { it.code == selectedCode }?.label ?: "Not selected"
    Column(
        modifier = Modifier.semantics { stateDescription = selectedLabel },
        verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
    ) {
        FieldLabel(definition.label)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s),
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
        ) {
            options.forEach { option ->
                BulletinOptionChip(
                    label = option.label,
                    selected = option.code == selectedCode,
                    onClick = { onInputChange(definition.key, if (option.code == selectedCode) "" else option.code) }
                )
            }
        }
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}
