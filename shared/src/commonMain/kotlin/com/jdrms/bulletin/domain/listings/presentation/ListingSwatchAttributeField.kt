package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.jdrms.bulletin.core.designsystem.BulletinColorSwatch
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDefinition
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeType
import com.jdrms.bulletin.domain.listings.domain.service.ListingMultiChoiceInput

/** Multi-select color swatches, the same control the marketplace color filter uses. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ListingSwatchAttributeField(
    definition: ListingAttributeDefinition,
    value: String,
    error: String?,
    onInputChange: (String, String) -> Unit
) {
    val options = (definition.type as ListingAttributeType.MultiChoice).options
    val selected = ListingMultiChoiceInput.decode(value).toSet()
    val summary = options.filter { it.code in selected }.joinToString { it.label }.ifEmpty { "Not selected" }
    Column(modifier = Modifier.semantics { stateDescription = summary }) {
        Text(
            text = "${definition.label} (choose any)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow {
            options.forEach { option ->
                BulletinColorSwatch(
                    code = option.code,
                    label = option.label,
                    selected = option.code in selected,
                    onClick = {
                        onInputChange(definition.key, ListingMultiChoiceInput.toggle(value, option.code, options))
                    }
                )
            }
        }
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}
