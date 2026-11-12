package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory

internal data class CreateListingPriceActions(
    val onPriceChange: (String) -> Unit,
    val onDescriptionChange: (String) -> Unit
)

@Composable
internal fun CreateListingPriceStep(
    state: ListingsUiState,
    actions: CreateListingPriceActions
) {
    val isHousing = state.newCategory == ListingCategory.HOUSING
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle(if (isHousing) "Rent & description" else CreateListingStep.PRICE.title)
        if (isHousing) {
            ListingHousingRentField(state.newPriceCents, actions.onPriceChange, Modifier.fillMaxWidth())
        } else {
            CurrencyTextField(
                digits = state.newPriceCents,
                onDigitsChange = actions.onPriceChange,
                required = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (isHousing) {
            ListingHousingDescriptionField(state.newDescription, actions.onDescriptionChange)
        } else {
            ListingItemDescriptionField(state.newDescription, actions.onDescriptionChange)
        }
        state.errorMessage?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
