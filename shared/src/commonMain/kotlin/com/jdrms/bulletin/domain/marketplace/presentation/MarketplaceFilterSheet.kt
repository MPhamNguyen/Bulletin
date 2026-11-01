package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    ModalBottomSheet(
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
                }
            )
            FilterOptionGroup(
                title = "Category",
                options = listOf<MarketplaceCategory?>(null) + MarketplaceCategory.entries,
                selected = draft.category,
                label = { it?.displayName() ?: "All" },
                onSelect = { draft = draft.copy(category = it).normalized() }
            )
            FilterOptionGroup(
                title = "Condition",
                options = listOf<MarketplaceCondition?>(null) + MarketplaceCondition.knownValues,
                selected = draft.condition,
                label = { it?.displayName() ?: "Any" },
                onSelect = { draft = draft.copy(condition = it) }
            )
            if (draft.category == MarketplaceCategory.ELECTRONICS) {
                MarketplaceAttributeFilterFields(attributeDraft, attributeErrors) { attributeDraft = it }
            }
            ShowMarketplaceResultsButton {
                val parsed = attributeDraft.toFilters()
                val attributes = parsed.filters.takeIf { draft.category == MarketplaceCategory.ELECTRONICS }
                attributeErrors = if (draft.category == MarketplaceCategory.ELECTRONICS) parsed.errors else emptyMap()
                if (attributeErrors.isEmpty()) {
                    coroutineScope.launch {
                        sheetState.hide()
                        onApply(draft.copy(attributes = attributes ?: MarketplaceAttributeFilters.NONE))
                    }
                }
            }
        }
    }
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
