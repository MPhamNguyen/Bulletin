package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import com.jdrms.bulletin.core.designsystem.BulletinOptionChip
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
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
    Column(verticalArrangement = Arrangement.spacedBy(BulletinSpacing.xl)) {
        SectionTitle(CreateListingStep.DETAILS.title)
        if (state.newCategory != ListingCategory.HOUSING) {
            Column(verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)) {
                FieldLabel("Condition")
                ConditionChips(state.newCondition, actions.onConditionSelected)
            }
        }
        ListingAttributeFields(
            category = state.newCategory,
            draft = state.newAttributes,
            onInputChange = actions.onAttributeChange
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConditionChips(selected: ListingCondition, onSelected: (ListingCondition) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s),
        verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
    ) {
        ListingCondition.entries.forEach { condition ->
            BulletinOptionChip(
                label = condition.displayLabel(),
                selected = condition == selected,
                onClick = { onSelected(condition) }
            )
        }
    }
}
