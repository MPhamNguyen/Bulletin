package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults
import com.jdrms.bulletin.core.designsystem.showKeyboardOnFocusOrTap
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDefinition
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeType
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeUnit
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingTextFormat
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputParser

/**
 * Optional item-detail fields for [category], shared by the create and edit forms. Shows nothing, and leaves no
 * gap, for categories without attributes. Choice fields are chips; tapping the selected chip clears it.
 */
@Composable
internal fun ListingAttributeFields(
    category: ListingCategory,
    draft: ListingAttributeDraft,
    onInputChange: (key: String, text: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val parser = remember { ListingAttributeInputParser() }
    val definitions = parser.applicableDefinitions(category, draft.inputs)
    if (definitions.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Item details (optional)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        definitions.forEach { definition ->
            val value = draft.inputs[definition.key].orEmpty()
            val error = draft.errors[definition.key]
            when (definition.type) {
                is ListingAttributeType.Choice -> ListingChoiceAttributeField(definition, value, error, onInputChange)
                is ListingAttributeType.Date ->
                    ListingDateAttributeField(category, definition, value, error, onInputChange)
                is ListingAttributeType.Text -> if (definition.type.format == ListingTextFormat.ISBN) {
                    ListingIsbnAttributeField(definition, value, error, onInputChange)
                } else {
                    ListingTextAttributeField(category, definition, value, error, onInputChange)
                }
                is ListingAttributeType.Decimal -> if (definition.type.unit == ListingAttributeUnit.USD_PER_MONTH) {
                    ListingRentAttributeField(definition, value, error, onInputChange)
                } else {
                    ListingTextAttributeField(category, definition, value, error, onInputChange)
                }
                else -> ListingTextAttributeField(category, definition, value, error, onInputChange)
            }
        }
        draft.errors[ListingAttributeInputParser.GENERAL_ERROR_KEY]?.let { message ->
            Text(text = message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ListingTextAttributeField(
    category: ListingCategory,
    definition: ListingAttributeDefinition,
    value: String,
    error: String?,
    onInputChange: (String, String) -> Unit
) {
    val helper = error ?: helperTextFor(definition)
    OutlinedTextField(
        value = value,
        onValueChange = { onInputChange(definition.key, definition.filterInput(it)) },
        label = { Text(ListingAttributeDisplay.fieldLabel(definition)) },
        supportingText = if (helper != null) {
            { Text(helper) }
        } else {
            null
        },
        isError = error != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardTypeFor(definition)),
        colors = BulletinTextFieldDefaults.colors(),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.showKeyboardOnFocusOrTap().fillMaxWidth()
    )
    val suggestions = ListingAttributeSuggestions.suggestionsFor(category, definition.key, value)
    if (suggestions.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(suggestions) { suggestion ->
                SuggestionChip(
                    onClick = { onInputChange(definition.key, suggestion) },
                    label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}

@Composable
private fun ListingChoiceAttributeField(
    definition: ListingAttributeDefinition,
    selectedCode: String,
    error: String?,
    onInputChange: (String, String) -> Unit
) {
    val options = (definition.type as ListingAttributeType.Choice).options
    val selectedLabel = options.firstOrNull { it.code == selectedCode }?.label ?: "Not selected"
    Column(modifier = Modifier.semantics { stateDescription = selectedLabel }) {
        Text(
            text = definition.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(options) { option ->
                FilterChip(
                    selected = option.code == selectedCode,
                    onClick = { onInputChange(definition.key, if (option.code == selectedCode) "" else option.code) },
                    label = { Text(option.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}

private fun keyboardTypeFor(definition: ListingAttributeDefinition): KeyboardType = when (definition.type) {
    is ListingAttributeType.Integer -> KeyboardType.Number
    is ListingAttributeType.Decimal -> KeyboardType.Decimal
    else -> KeyboardType.Text
}

private fun helperTextFor(definition: ListingAttributeDefinition): String? {
    val unit = (definition.type as? ListingAttributeType.Decimal)?.unit
    return if (unit == ListingAttributeUnit.USD_PER_MONTH) "Rent per month, separate from the listing price." else null
}
