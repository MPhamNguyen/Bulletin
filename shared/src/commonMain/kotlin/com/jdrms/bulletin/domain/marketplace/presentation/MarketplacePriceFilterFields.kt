package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jdrms.bulletin.core.designsystem.MoneyInput
import com.jdrms.bulletin.core.designsystem.MoneyInputAppearance
import com.jdrms.bulletin.core.designsystem.theme.BulletinFieldStyle
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePriceRange

internal data class MarketplacePriceFilterDraft(
    val minimumCents: String = "",
    val maximumCents: String = ""
) {
    fun toPriceRange(): MarketplacePriceFilterResult {
        val minimum = minimumCents.toLongOrNull()
        val maximum = maximumCents.toLongOrNull()
        return if (minimum != null && maximum != null && minimum > maximum) {
            MarketplacePriceFilterResult(error = "Maximum price must be at least the minimum price.")
        } else {
            MarketplacePriceFilterResult(priceRange = MarketplacePriceRange(minimum, maximum))
        }
    }

    companion object {
        fun from(priceRange: MarketplacePriceRange) = MarketplacePriceFilterDraft(
            minimumCents = priceRange.minimumCents?.toString().orEmpty(),
            maximumCents = priceRange.maximumCents?.toString().orEmpty()
        )
    }
}

internal data class MarketplacePriceFilterResult(
    val priceRange: MarketplacePriceRange = MarketplacePriceRange.NONE,
    val error: String? = null
)

@Composable
internal fun MarketplacePriceFilterFields(
    draft: MarketplacePriceFilterDraft,
    error: String?,
    onDraftChanged: (MarketplacePriceFilterDraft) -> Unit
) {
    var minimumError by remember { mutableStateOf<String?>(null) }
    var maximumError by remember { mutableStateOf<String?>(null) }
    MarketplaceRangeFields(
        title = "Price",
        error = minimumError ?: maximumError ?: error,
        minimumField = { modifier, placeholder ->
            MoneyInput(
                digits = draft.minimumCents,
                onDigitsChange = { onDraftChanged(draft.copy(minimumCents = it)) },
                modifier = modifier,
                appearance = MoneyInputAppearance(
                    currencySymbolAsAdornment = true,
                    placeholder = placeholder,
                    style = BulletinFieldStyle.Inset
                ),
                onInputError = { minimumError = it }
            )
        },
        maximumField = { modifier, placeholder ->
            MoneyInput(
                digits = draft.maximumCents,
                onDigitsChange = { onDraftChanged(draft.copy(maximumCents = it)) },
                modifier = modifier,
                appearance = MoneyInputAppearance(
                    currencySymbolAsAdornment = true,
                    placeholder = placeholder,
                    style = BulletinFieldStyle.Inset
                ),
                onInputError = { maximumError = it }
            )
        }
    )
}
