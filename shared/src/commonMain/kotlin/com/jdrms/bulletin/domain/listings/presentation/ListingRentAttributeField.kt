package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.MoneyInput
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDefinition

@Composable
internal fun ListingRentAttributeField(
    definition: ListingAttributeDefinition,
    value: String,
    error: String?,
    onInputChange: (String, String) -> Unit
) {
    MoneyInput(
        digits = rentAmountToDigits(value),
        onDigitsChange = { onInputChange(definition.key, rentDigitsToAmount(it)) },
        label = { Text(ListingAttributeDisplay.fieldLabel(definition)) },
        error = error,
        modifier = Modifier.fillMaxWidth()
    )
}
