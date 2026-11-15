package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.jdrms.bulletin.core.designsystem.MoneyInput
import com.jdrms.bulletin.core.designsystem.MoneyInputAppearance
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory

/** One required rent amount backs the listing price and housing rent metadata in create and edit flows. */
@Composable
internal fun ListingHousingRentField(
    digits: String,
    onDigitsChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val error = parseListingFormAttributes(ListingCategory.HOUSING, ListingAttributeDraft(), digits).rentError
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)) {
        ListingRequiredLabel("Monthly rent (USD / month)", style = MaterialTheme.typography.titleSmall)
        MoneyInput(
            digits = digits,
            onDigitsChange = onDigitsChange,
            appearance = MoneyInputAppearance(placeholder = "$"),
            error = error,
            modifier = Modifier.semantics { contentDescription = "Monthly rent" }
        )
    }
}
