package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinFieldDecoration
import com.jdrms.bulletin.core.designsystem.BulletinFieldOptions
import com.jdrms.bulletin.core.designsystem.BulletinTextField
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
        CurrencyTextField(
            digits = state.newPriceCents,
            onDigitsChange = actions.onPriceChange,
            required = true,
            textStyle = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth()
        )
        BulletinTextField(
            value = state.newDescription,
            onValueChange = actions.onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            decoration = BulletinFieldDecoration(
                label = { ListingRequiredLabel("Description & Campus Pick-Up Details") },
                placeholder = { Text("Wear, what's included, where to meet.") }
            ),
            options = BulletinFieldOptions(
                minLines = 3,
                maxLines = 6
            )
        )
        state.errorMessage?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
