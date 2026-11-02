package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class ListingSearchFilterState(
    val query: String = "",
    val category: String? = null,
    val condition: String? = null
)

@Composable
fun ListingSearchFilters(
    state: ListingSearchFilterState,
    categories: List<ListingFilterOption>,
    conditions: List<ListingFilterOption>,
    onQueryChanged: (String) -> Unit,
    onFiltersApplied: (ListingSearchFilterState) -> Unit,
    modifier: Modifier = Modifier
) {
    var isSheetOpen by remember { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ListingSearchBar(state, onQueryChanged, isSheetOpen) { isSheetOpen = true }
        if (isSheetOpen) {
            ListingFiltersSheet(
                state = state,
                categories = categories,
                conditions = conditions,
                onApply = {
                    onFiltersApplied(it)
                    isSheetOpen = false
                },
                onDismiss = { isSheetOpen = false }
            )
        }
        ActiveListingFilterChips(state, categories, conditions, onFiltersApplied)
    }
}

@Composable
private fun ListingSearchBar(
    state: ListingSearchFilterState,
    onQueryChanged: (String) -> Unit,
    isSheetOpen: Boolean,
    onShowFilters: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChanged,
            placeholder = { Text("Search textbooks, furniture, etc.") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.showKeyboardOnFocusOrTap().weight(1f)
                .semantics { contentDescription = "Search listings" }
        )
        ListingFilterButton(state.activeFilterCount, isSheetOpen, onShowFilters)
    }
}

@Composable
private fun ListingFilterButton(activeCount: Int, isExpanded: Boolean, onClick: () -> Unit) {
    BadgedBox(badge = { if (activeCount > 0) Badge { Text(activeCount.toString()) } }) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.semantics {
                contentDescription = when {
                    isExpanded -> "Hide filters"
                    activeCount == 0 -> "Show filters"
                    else -> "Show filters, $activeCount active"
                }
            },
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Outlined.Tune, contentDescription = null)
        }
    }
}

@Composable
private fun ActiveListingFilterChips(
    state: ListingSearchFilterState,
    categories: List<ListingFilterOption>,
    conditions: List<ListingFilterOption>,
    onApply: (ListingSearchFilterState) -> Unit
) {
    if (state.activeFilterCount == 0) return
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        state.category?.let { value ->
            FilterChip(
                selected = true,
                onClick = { onApply(state.copy(category = null)) },
                label = { Text("${categories.labelFor(value)} ×") }
            )
        }
        state.condition?.let { value ->
            FilterChip(
                selected = true,
                onClick = { onApply(state.copy(condition = null)) },
                label = { Text("${conditions.labelFor(value)} ×") }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListingFiltersSheet(
    state: ListingSearchFilterState,
    categories: List<ListingFilterOption>,
    conditions: List<ListingFilterOption>,
    onApply: (ListingSearchFilterState) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember(state) { mutableStateOf(state) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        ListingFiltersSheetContent(
            draft = draft,
            categories = categories,
            conditions = conditions,
            onDraftChanged = { draft = it },
            onApply = {
                scope.launch {
                    sheetState.hide()
                    onApply(draft)
                }
            }
        )
    }
}

@Composable
private fun ListingFiltersSheetContent(
    draft: ListingSearchFilterState,
    categories: List<ListingFilterOption>,
    conditions: List<ListingFilterOption>,
    onDraftChanged: (ListingSearchFilterState) -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ListingFiltersHeader { onDraftChanged(ListingSearchFilterState(query = draft.query)) }
        ListingFilterOptionGroup("Category", "All", categories, draft.category) {
            onDraftChanged(draft.copy(category = it))
        }
        ListingFilterOptionGroup("Condition", "Any", conditions, draft.condition) {
            onDraftChanged(draft.copy(condition = it))
        }
        Button(onClick = onApply, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text("Show results")
        }
    }
}

@Composable
private fun ListingFiltersHeader(onReset: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Filters", style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = onReset) { Text("Reset all") }
    }
}

@Composable
private fun ListingFilterOptionGroup(
    title: String,
    allLabel: String,
    options: List<ListingFilterOption>,
    selected: String?,
    onSelected: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (listOf<ListingFilterOption?>(null) + options).forEach { option ->
                FilterChip(
                    selected = option?.value == selected,
                    onClick = { onSelected(option?.value) },
                    label = { Text(option?.label ?: allLabel) }
                )
            }
        }
    }
}

private val ListingSearchFilterState.activeFilterCount: Int
    get() = listOfNotNull(category, condition).size

private fun List<ListingFilterOption>.labelFor(value: String): String =
    firstOrNull { it.value == value }?.label ?: value
