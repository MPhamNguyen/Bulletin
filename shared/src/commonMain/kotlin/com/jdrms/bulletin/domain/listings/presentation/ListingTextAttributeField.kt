package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import com.jdrms.bulletin.core.designsystem.BulletinFieldDecoration
import com.jdrms.bulletin.core.designsystem.BulletinFieldOptions
import com.jdrms.bulletin.core.designsystem.BulletinOptionChip
import com.jdrms.bulletin.core.designsystem.BulletinTextField
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDefinition
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeKeys
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeType
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ListingTextAttributeField(
    category: ListingCategory,
    definition: ListingAttributeDefinition,
    value: String,
    error: String?,
    onInputChange: (String, String) -> Unit
) {
    val placeholder = when (category) {
        ListingCategory.FURNITURE -> when (definition.key) {
            ListingAttributeKeys.WIDTH_CM -> "e.g., 80.0"
            ListingAttributeKeys.LENGTH_CM -> "e.g., 120.5"
            else -> null
        }
        ListingCategory.TEXTBOOKS -> when (definition.key) {
            ListingAttributeKeys.COURSE_CODE -> "e.g., CECS 329"
            ListingAttributeKeys.EDITION -> "e.g., 8th edition"
            else -> null
        }
        else -> null
    }
    val helper = error
    Column(verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)) {
        FieldLabel(ListingAttributeDisplay.fieldLabel(definition))
        BulletinTextField(
            value = value,
            onValueChange = { onInputChange(definition.key, definition.filterInput(it)) },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = definition.label },
            isError = error != null,
            decoration = BulletinFieldDecoration(
                placeholder = placeholder?.let { { Text(it) } },
                supportingText = if (helper != null) {
                    { Text(helper) }
                } else {
                    null
                }
            ),
            options = BulletinFieldOptions(
                keyboardOptions = KeyboardOptions(keyboardType = keyboardTypeFor(definition))
            )
        )
        val suggestions = ListingAttributeSuggestions.suggestionsFor(category, definition.key, value)
        if (suggestions.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s),
                verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
            ) {
                suggestions.forEach { suggestion ->
                    BulletinOptionChip(
                        label = suggestion,
                        selected = value == suggestion,
                        onClick = { onInputChange(definition.key, suggestion) }
                    )
                }
            }
        }
    }
}

private fun keyboardTypeFor(definition: ListingAttributeDefinition): KeyboardType = when (definition.type) {
    is ListingAttributeType.Integer -> KeyboardType.Number
    is ListingAttributeType.Decimal -> KeyboardType.Decimal
    else -> KeyboardType.Text
}
