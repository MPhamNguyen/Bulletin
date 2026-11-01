package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import com.jdrms.bulletin.core.designsystem.MoneyInput

@Composable
internal fun CurrencyTextField(
    digits: String,
    onDigitsChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    shape: Shape = MaterialTheme.shapes.small,
    textStyle: TextStyle = LocalTextStyle.current
) {
    MoneyInput(
        digits = digits,
        onDigitsChange = onDigitsChange,
        label = { if (required) ListingRequiredLabel("Price (USD)") else Text("Price (USD)") },
        modifier = modifier,

        shape = shape,
        textStyle = textStyle
    )
}
