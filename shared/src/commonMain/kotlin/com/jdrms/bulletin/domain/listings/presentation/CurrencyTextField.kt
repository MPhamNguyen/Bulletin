package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.jdrms.bulletin.core.designsystem.MoneyInput
import com.jdrms.bulletin.core.designsystem.MoneyInputAppearance
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

@Composable
internal fun CurrencyTextField(
    digits: String,
    onDigitsChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)) {
        if (required) {
            ListingRequiredLabel("Price (USD)", style = MaterialTheme.typography.titleSmall)
        } else {
            FieldLabel("Price (USD)")
        }
        MoneyInput(
            digits = digits,
            onDigitsChange = onDigitsChange,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Price (USD)" },
            appearance = MoneyInputAppearance(placeholder = "$", textStyle = textStyle)
        )
    }
}
