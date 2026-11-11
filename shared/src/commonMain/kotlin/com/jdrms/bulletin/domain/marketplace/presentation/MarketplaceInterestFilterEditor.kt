package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSheetDefaults
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterest
import com.jdrms.bulletin.domain.marketplace.domain.model.supportsCondition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MarketplaceInterestFilterEditor(
    interest: MarketplaceInterest,
    onApply: (MarketplaceInterest) -> Unit,
    onDismiss: () -> Unit
) {
    var condition by remember(interest) { mutableStateOf(interest.condition) }
    var priceDraft by remember(interest) { mutableStateOf(MarketplacePriceFilterDraft.from(interest.priceRange)) }
    var priceError by remember(interest) { mutableStateOf<String?>(null) }
    var attributeDraft by remember(interest) {
        mutableStateOf(MarketplaceAttributeFilterDraft.from(interest.attributes))
    }
    var attributeErrors by remember(interest) { mutableStateOf(emptyMap<String, String>()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        modifier = Modifier.fillMaxHeight(),
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        shape = BulletinSheetDefaults.shape,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = BulletinSpacing.l)
                .padding(bottom = BulletinSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.l)
        ) {
            Text(interest.category.displayName(), style = MaterialTheme.typography.titleLarge)
            if (interest.category.supportsCondition) {
                InterestConditionFilters(condition) { condition = it }
            }
            MarketplacePriceFilterFields(priceDraft, priceError) {
                priceDraft = it
                priceError = null
            }
            if (interest.category != MarketplaceCategory.OTHER) {
                MarketplaceAttributeFilterFields(
                    interest.category,
                    attributeDraft,
                    attributeErrors
                ) { attributeDraft = it }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
            ) {
                BulletinButton(
                    onClick = {
                        condition = null
                        priceDraft = MarketplacePriceFilterDraft()
                        priceError = null
                        attributeDraft = MarketplaceAttributeFilterDraft()
                        attributeErrors = emptyMap()
                    },
                    modifier = Modifier.weight(1f),
                    variant = BulletinButtonVariant.Outlined
                ) {
                    Text("Reset")
                }
                BulletinButton(
                    onClick = {
                        val parsedPrice = priceDraft.toPriceRange()
                        val parsedAttributes = parseAttributes(interest.category, attributeDraft)
                        priceError = parsedPrice.error
                        attributeErrors = parsedAttributes.errors
                        if (priceError == null && attributeErrors.isEmpty()) {
                            onApply(
                                interest.copy(
                                    condition = condition,
                                    priceRange = parsedPrice.priceRange,
                                    attributes = parsedAttributes.filters ?: MarketplaceAttributeFilters.NONE
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    variant = BulletinButtonVariant.Primary
                ) {
                    Text("Apply")
                }
            }
        }
    }
}

@Composable
private fun InterestConditionFilters(
    selected: MarketplaceCondition?,
    onSelect: (MarketplaceCondition?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(BulletinSpacing.xs)) {
        Text("Condition", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
        ) {
            (listOf<MarketplaceCondition?>(null) + MarketplaceCondition.knownValues).forEach { condition ->
                FilterChip(
                    selected = selected == condition,
                    onClick = { onSelect(condition) },
                    label = { Text(condition?.displayName() ?: "Any") }
                )
            }
        }
    }
}

private fun parseAttributes(
    category: MarketplaceCategory,
    draft: MarketplaceAttributeFilterDraft
): MarketplaceAttributeFilterDraft.Result = if (category == MarketplaceCategory.OTHER) {
    MarketplaceAttributeFilterDraft.Result(MarketplaceAttributeFilters.NONE, emptyMap())
} else {
    draft.toFilters(category)
}
