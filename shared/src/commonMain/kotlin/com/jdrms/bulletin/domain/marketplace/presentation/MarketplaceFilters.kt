package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition

@Composable
fun MarketplaceFilters(
    filters: MarketplaceFilterState,
    onCategorySelected: (MarketplaceCategory?) -> Unit,
    onConditionSelected: (MarketplaceCondition?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MarketplaceFilterMenu(
            label = "Categories",
            selectedLabel = filters.category?.displayName(),
            options = MarketplaceCategory.entries.map { it.displayName() },
            onSelected = { index -> onCategorySelected(index?.let(MarketplaceCategory.entries::get)) },
            modifier = Modifier.weight(1f)
        )
        MarketplaceFilterMenu(
            label = "Conditions",
            selectedLabel = filters.condition?.displayName(),
            options = MarketplaceCondition.entries.map { it.displayName() },
            onSelected = { index -> onConditionSelected(index?.let(MarketplaceCondition.entries::get)) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MarketplaceFilterMenu(
    label: String,
    selectedLabel: String?,
    options: List<String>,
    onSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedLabel ?: "All $label")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("All $label") },
                onClick = {
                    onSelected(null)
                    expanded = false
                }
            )
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun MarketplaceCategory.displayName(): String = name.toDisplayName()

private fun MarketplaceCondition.displayName(): String = name.toDisplayName()

private fun String.toDisplayName(): String = lowercase()
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }
