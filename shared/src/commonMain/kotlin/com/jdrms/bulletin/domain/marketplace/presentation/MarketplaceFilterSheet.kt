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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSheetDefaults
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceSortOrder
import com.jdrms.bulletin.domain.marketplace.domain.model.supportsCondition
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MarketplaceFilterSheet(
    filters: MarketplaceFilterState,
    onApply: (MarketplaceFilterState) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember(filters) { mutableStateOf(filters) }
    var attributeDraft by remember(filters) { mutableStateOf(MarketplaceAttributeFilterDraft.from(filters.attributes)) }
    var attributeErrors by remember(filters) { mutableStateOf(emptyMap<String, String>()) }
    var priceDraft by remember(filters) { mutableStateOf(MarketplacePriceFilterDraft.from(filters.priceRange)) }
    var priceError by remember(filters) { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MarketplaceFilterCardHeader(
                onReset = {
                    draft = MarketplaceFilterState(query = draft.query)
                    attributeDraft = MarketplaceAttributeFilterDraft()
                    attributeErrors = emptyMap()
                    priceDraft = MarketplacePriceFilterDraft()
                    priceError = null
                }
            )
            FilterOptionGroup(
                title = "Category",
                options = listOf<MarketplaceCategory?>(null) + MarketplaceCategory.entries,
                selected = draft.category,
                label = { it?.displayName() ?: "All" },
                onSelect = {
                    if (draft.category != it) {
                        attributeDraft = MarketplaceAttributeFilterDraft()
                        attributeErrors = emptyMap()
                    }
                    draft = draft.copy(
                        category = it,
                        attributes = MarketplaceAttributeFilters.NONE
                    ).normalized()
                }
            )
            if (draft.category?.supportsCondition != false) {
                FilterOptionGroup(
                    title = "Condition",
                    options = listOf<MarketplaceCondition?>(null) + MarketplaceCondition.knownValues,
                    selected = draft.condition,
                    label = { it?.displayName() ?: "Any" },
                    onSelect = { draft = draft.copy(condition = it) }
                )
            }
            MarketplacePriceFilterFields(
                draft = priceDraft,
                error = priceError,
                onDraftChanged = {
                    priceDraft = it
                    priceError = null
                }
            )
            FilterOptionGroup(
                title = "Sort by",
                options = MarketplaceSortOrder.entries,
                selected = draft.sortOrder,
                label = { it?.displayName() ?: MarketplaceSortOrder.NEWEST.displayName() },
                onSelect = { draft = draft.copy(sortOrder = it ?: MarketplaceSortOrder.NEWEST) }
            )
            draft.category?.takeUnless { it == MarketplaceCategory.OTHER }?.let { category ->
                MarketplaceAttributeFilterFields(category, attributeDraft, attributeErrors) { attributeDraft = it }
            }
            ShowMarketplaceResultsButton {
                val parsedPrice = priceDraft.toPriceRange()
                val parsed = draft.category?.takeUnless { it == MarketplaceCategory.OTHER }
                    ?.let(attributeDraft::toFilters)
                val attributes = parsed?.filters ?: MarketplaceAttributeFilters.NONE
                attributeErrors = parsed?.errors.orEmpty()
                priceError = parsedPrice.error
                if (attributeErrors.isEmpty() && priceError == null) {
                    coroutineScope.launch {
                        sheetState.hide()
                        onApply(draft.copy(attributes = attributes, priceRange = parsedPrice.priceRange))
                    }
                }
            }
        }
    }
}

internal fun MarketplaceSortOrder.displayName(): String = when (this) {
    MarketplaceSortOrder.NEWEST -> "Newest"
    MarketplaceSortOrder.PRICE_LOW_TO_HIGH -> "Price: low to high"
    MarketplaceSortOrder.PRICE_HIGH_TO_LOW -> "Price: high to low"
}

@Composable
private fun MarketplaceFilterCardHeader(onReset: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Filters", style = MaterialTheme.typography.titleLarge)
        BulletinButton(onClick = onReset, variant = BulletinButtonVariant.Text) { Text("Reset all") }
    }
}

@Composable
private fun ShowMarketplaceResultsButton(onClick: () -> Unit) {
    BulletinButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        variant = BulletinButtonVariant.Primary
    ) {
        Text("Show results")
    }
}

@Composable
private fun <T> FilterOptionGroup(
    title: String,
    options: List<T?>,
    selected: T?,
    label: (T?) -> String,
    onSelect: (T?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(label(option)) }
                )
            }
        }
    }
}
