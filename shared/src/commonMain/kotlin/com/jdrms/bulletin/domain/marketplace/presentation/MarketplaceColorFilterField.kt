package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import com.jdrms.bulletin.core.designsystem.BulletinColorSwatch
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeSpec

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MarketplaceColorFilterField(
    spec: MarketplaceAttributeSpec,
    input: MarketplaceAttributeFilterInput,
    onToggle: (String) -> Unit
) {
    MarketplaceFilterSection(title = spec.label, hint = "Choose any colors") {
        FlowRow {
            spec.choices.forEach { (code, label) ->
                BulletinColorSwatch(
                    code = code,
                    label = label,
                    selected = code in input.choices || code == input.exact,
                    onClick = { onToggle(code) }
                )
            }
        }
    }
}
