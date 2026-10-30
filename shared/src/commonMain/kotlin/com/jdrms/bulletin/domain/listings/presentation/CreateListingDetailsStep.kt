package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingCondition

internal data class CreateListingDetailsActions(
    val onConditionSelected: (ListingCondition) -> Unit,
    val onAttributeChange: (key: String, text: String) -> Unit
)

@Composable
internal fun CreateListingDetailsStep(
    state: ListingsUiState,
    actions: CreateListingDetailsActions
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle(CreateListingStep.DETAILS.title)
        if (state.newCategory != ListingCategory.HOUSING) {
            FieldLabel("Condition")
            ConditionChips(state.newCondition, actions.onConditionSelected)
        }
        ListingAttributeFields(
            category = state.newCategory,
            draft = state.newAttributes,
            onInputChange = actions.onAttributeChange
        )
    }
}

@Composable
private fun ConditionChips(selected: ListingCondition, onSelected: (ListingCondition) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ListingCondition.entries.toList()) { condition ->
            FilterChip(
                selected = condition == selected,
                onClick = { onSelected(condition) },
                label = { Text(condition.displayLabel(), style = MaterialTheme.typography.bodyMedium) },
                shape = MaterialTheme.shapes.extraLarge,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = scheme.surfaceVariant,
                    selectedContainerColor = scheme.primaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = condition == selected,
                    borderColor = scheme.surfaceVariant,
                    selectedBorderColor = scheme.primary,
                    borderWidth = 1.5.dp,
                    selectedBorderWidth = 1.5.dp
                )
            )
        }
    }
}
