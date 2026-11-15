package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.runtime.Composable

@Composable
internal fun MarketplaceSizeFilterField(
    choices: Map<String, String>,
    input: MarketplaceAttributeFilterInput,
    onToggle: (String) -> Unit
) {
    MarketplaceFilterSection(title = "Size", hint = "Choose any sizes") {
        MarketplacePillGroup(
            options = choices.keys.toList(),
            isSelected = { it in input.choices || it == input.exact },
            label = { choices.getValue(it) },
            onToggle = onToggle
        )
    }
}
