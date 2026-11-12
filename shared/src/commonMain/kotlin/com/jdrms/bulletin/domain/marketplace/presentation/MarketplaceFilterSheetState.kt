package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceSortOrder

/**
 * Draft of the filter sheet. Nothing reaches the screen until [apply] succeeds.
 * When [ownsCategoryAndSort] is false the main screen owns category and sort, so the draft keeps both untouched.
 */
internal class MarketplaceFilterSheetState(
    initial: MarketplaceFilterState,
    val ownsCategoryAndSort: Boolean
) {
    var draft by mutableStateOf(initial)
        private set
    var attributeDraft by mutableStateOf(MarketplaceAttributeFilterDraft.from(initial.attributes))
        private set
    var priceDraft by mutableStateOf(MarketplacePriceFilterDraft.from(initial.priceRange))
        private set
    var attributeErrors by mutableStateOf(emptyMap<String, String>())
        private set
    var priceError by mutableStateOf<String?>(null)
        private set

    /** Category whose attribute fields are shown; Other has none. */
    val attributeCategory: MarketplaceCategory?
        get() = draft.category?.takeUnless { it == MarketplaceCategory.OTHER }

    val hasActiveFilters: Boolean
        get() = draft.condition != null ||
            priceDraft != MarketplacePriceFilterDraft() ||
            attributeDraft.inputs.values.any { it != MarketplaceAttributeFilterInput() } ||
            (ownsCategoryAndSort && (draft.category != null || draft.sortOrder != MarketplaceSortOrder.NEWEST))

    fun selectCondition(condition: MarketplaceCondition?) {
        draft = draft.copy(condition = condition)
    }

    fun selectSortOrder(sortOrder: MarketplaceSortOrder) {
        draft = draft.copy(sortOrder = sortOrder)
    }

    fun selectCategory(category: MarketplaceCategory?) {
        if (draft.category == category) return
        attributeDraft = MarketplaceAttributeFilterDraft()
        attributeErrors = emptyMap()
        draft = draft.copy(category = category, attributes = MarketplaceAttributeFilters.NONE).normalized()
    }

    fun updateAttributes(next: MarketplaceAttributeFilterDraft) {
        attributeDraft = next
    }

    fun updatePrice(next: MarketplacePriceFilterDraft) {
        priceDraft = next
        priceError = null
    }

    fun reset() {
        draft = MarketplaceFilterState(
            query = draft.query,
            category = draft.category.takeUnless { ownsCategoryAndSort },
            sortOrder = if (ownsCategoryAndSort) MarketplaceSortOrder.NEWEST else draft.sortOrder
        )
        attributeDraft = MarketplaceAttributeFilterDraft()
        attributeErrors = emptyMap()
        priceDraft = MarketplacePriceFilterDraft()
        priceError = null
    }

    /** Returns the filters to apply, or null after surfacing field errors. */
    fun apply(): MarketplaceFilterState? {
        val parsedPrice = priceDraft.toPriceRange()
        val parsedAttributes = attributeCategory?.let(attributeDraft::toFilters)
        attributeErrors = parsedAttributes?.errors.orEmpty()
        priceError = parsedPrice.error
        if (attributeErrors.isNotEmpty() || priceError != null) return null
        return draft.copy(
            attributes = parsedAttributes?.filters ?: MarketplaceAttributeFilters.NONE,
            priceRange = parsedPrice.priceRange
        )
    }
}
