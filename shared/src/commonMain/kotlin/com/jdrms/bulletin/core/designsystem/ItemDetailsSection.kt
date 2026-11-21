package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class ItemDetailRow(val label: String, val value: String)

/** Shared loaded/placeholder label-value layout. Known rows remain visible during refresh.
 * Empty loaded sections emit nothing; unknown sections reserve three text-scaled rows.
 */
@Composable
fun ItemDetailsSection(
    rows: List<ItemDetailRow>,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    if (rows.isEmpty() && !isLoading) return
    val showPlaceholders = isLoading && rows.isEmpty()
    val displayedRows = if (showPlaceholders) List(3) { ItemDetailRow("Attribute", "Item value") } else rows
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Item details",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        displayedRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = row.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(120.dp).skeleton(showPlaceholders)
                )
                Text(
                    text = row.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.widthIn(min = 0.dp).skeleton(showPlaceholders)
                )
            }
        }
    }
}
