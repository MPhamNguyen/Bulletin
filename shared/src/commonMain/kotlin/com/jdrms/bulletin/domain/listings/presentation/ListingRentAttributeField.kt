package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.jdrms.bulletin.core.designsystem.MoneyInput
import com.jdrms.bulletin.core.designsystem.MoneyInputAppearance
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDefinition

@Composable
internal fun ListingRentAttributeField(
    definition: ListingAttributeDefinition,
    value: String,
    error: String?,
    onInputChange: (String, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)) {
        FieldLabel(ListingAttributeDisplay.fieldLabel(definition))
        MoneyInput(
            digits = rentAmountToDigits(value),
            onDigitsChange = { onInputChange(definition.key, rentDigitsToAmount(it)) },
            appearance = MoneyInputAppearance(placeholder = ""),
            error = error,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = definition.label }
        )
    }
}
