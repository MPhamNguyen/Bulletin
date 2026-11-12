package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
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
    val fields = listingAttributeFormFields(category, draft.inputs)
    if (fields.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(BulletinSpacing.xl)) {
        fields.forEach { field ->
            val definition = field.definition
            val value = draft.inputs[definition.key].orEmpty()
            val error = draft.errors[definition.key]
            when (definition.type) {
                is ListingAttributeType.Choice -> ListingChoiceAttributeField(
                    definition,
                    definition.choiceOptions(draft.inputs),
                    value,
                    error,
                    onInputChange
                )
                is ListingAttributeType.MultiChoice ->
                    ListingSwatchAttributeField(definition, value, error, onInputChange)
                is ListingAttributeType.Integer -> if (definition.type.allowedValues.isNotEmpty()) {
                    ListingCapacityAttributeField(
                        definition,
                        value,
                        error,
                        onInputChange,
                        enabled = field.enabled,
                        unavailableReason = field.unavailableReason
                    )
                } else {
                    ListingTextAttributeField(category, definition, value, error, onInputChange)
                }
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
            }
        }
        draft.errors[ListingAttributeInputParser.GENERAL_ERROR_KEY]?.let { message ->
            Text(text = message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
