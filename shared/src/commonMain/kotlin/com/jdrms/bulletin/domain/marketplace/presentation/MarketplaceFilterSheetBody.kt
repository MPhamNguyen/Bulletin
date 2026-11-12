package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceSortOrder
import com.jdrms.bulletin.domain.marketplace.domain.model.supportsCondition

/** Price, condition, then the selected category's own filters under a divider. */
@Composable
internal fun MarketplaceFilterSheetBody(state: MarketplaceFilterSheetState) {
    val category = state.draft.category
    Column(verticalArrangement = Arrangement.spacedBy(MARKETPLACE_FILTER_SECTION_GAP)) {
        if (state.ownsCategoryAndSort) CategoryAndSortSections(state)
        MarketplacePriceFilterFields(
            draft = state.priceDraft,
            error = state.priceError,
            onDraftChanged = state::updatePrice
        )
        if (category?.supportsCondition != false) {
            MarketplaceFilterSection(title = "Condition") {
                MarketplacePillGroup(
                    options = MarketplaceCondition.knownValues,
                    isSelected = { it == state.draft.condition },
                    label = { it.displayName() },
                    onToggle = { state.selectCondition(if (state.draft.condition == it) null else it) }
                )
            }
        }
        CategoryDetails(state, category)
    }
}

@Composable
private fun CategoryAndSortSections(state: MarketplaceFilterSheetState) {
    Column(verticalArrangement = Arrangement.spacedBy(MARKETPLACE_FILTER_SECTION_GAP)) {
        MarketplaceFilterSection(title = "Category") {
            MarketplacePillGroup(
                options = MarketplaceCategory.entries,
                isSelected = { it == state.draft.category },
                label = { it.displayName() },
                onToggle = { state.selectCategory(if (state.draft.category == it) null else it) }
            )
        }
        MarketplaceFilterSection(title = "Sort by") {
            MarketplacePillGroup(
                options = MarketplaceSortOrder.entries,
                isSelected = { it == state.draft.sortOrder },
                label = { it.displayName() },
                onToggle = state::selectSortOrder
            )
        }
    }
}

@Composable
private fun CategoryDetails(state: MarketplaceFilterSheetState, category: MarketplaceCategory?) {
    val attributeCategory = state.attributeCategory
    if (attributeCategory == null) {
        Text(
            text = when {
                category != null -> "No extra filters for this category."
                state.ownsCategoryAndSort -> "Pick a category above to unlock more filters, like brand, type or course."
                else -> NO_CATEGORY_NOTE
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(MARKETPLACE_FILTER_SECTION_GAP)) {
        MarketplaceFilterDivider("${attributeCategory.displayName()} details")
        MarketplaceAttributeFilterFields(
            category = attributeCategory,
            draft = state.attributeDraft,
            errors = state.attributeErrors,
            onChange = state::updateAttributes
        )
    }
}

private const val NO_CATEGORY_NOTE =
    "Pick a category on the main screen to unlock more filters, like brand, type or course."
