package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.MoneyInput
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
    ) {
        MoneyInput(
            digits = draft.minimumCents,
            onDigitsChange = { onDraftChanged(draft.copy(minimumCents = it)) },
            modifier = Modifier.weight(1f),
            label = { Text("Min price") }
        )
        MoneyInput(
            digits = draft.maximumCents,
            onDigitsChange = { onDraftChanged(draft.copy(maximumCents = it)) },
            modifier = Modifier.weight(1f),
            label = { Text("Max price") },
            error = error
        )
    }
}
