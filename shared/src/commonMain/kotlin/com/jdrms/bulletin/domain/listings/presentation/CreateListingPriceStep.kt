package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults
import com.jdrms.bulletin.core.designsystem.showKeyboardOnFocusOrTap
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
            shape = MaterialTheme.shapes.medium,
            textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.newDescription,
            onValueChange = actions.onDescriptionChange,
            label = { ListingRequiredLabel("Description & Campus Pick-Up Details") },
            placeholder = { Text("Wear, what's included, where to meet.") },
            minLines = 3,
            maxLines = 6,
            colors = BulletinTextFieldDefaults.colors(),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.showKeyboardOnFocusOrTap().fillMaxWidth()
        )
        state.errorMessage?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
