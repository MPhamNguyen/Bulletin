package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.runtime.Composable
import com.jdrms.bulletin.core.designsystem.MoneyInput
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
    MarketplaceRangeFields(
        title = "Price",
        error = error,
        minimumField = { modifier ->
            MoneyInput(
                digits = draft.minimumCents,
                onDigitsChange = { onDraftChanged(draft.copy(minimumCents = it)) },
                modifier = modifier,
                currencySymbolAsAdornment = true
            )
        },
        maximumField = { modifier ->
            MoneyInput(
                digits = draft.maximumCents,
                onDigitsChange = { onDraftChanged(draft.copy(maximumCents = it)) },
                modifier = modifier,
                currencySymbolAsAdornment = true
            )
        }
    )
}
