package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceLayoutPreference

@Composable
internal fun MarketplaceLayoutDropdown(
    selected: MarketplaceLayoutPreference,
    onSelected: (MarketplaceLayoutPreference) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedIconButton(
            onClick = { expanded = true },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(40.dp)
        ) {
            LayoutColumnsIcon(selected.columns, "${layoutOptionLabel(selected)}. Change listing layout")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MarketplaceLayoutPreference.entries.forEach { preference ->
                DropdownMenuItem(
                    text = { Text(layoutOptionLabel(preference)) },
                    leadingIcon = { LayoutColumnsIcon(preference.columns) },
                    onClick = {
                        expanded = false
                        onSelected(preference)
                    }
                )
            }
        }
    }
}

@Composable
private fun LayoutColumnsIcon(columns: Int, description: String? = null) {
    val squareSize = when (columns) {
        1 -> 16.dp
        2 -> 9.dp
        else -> 6.dp
    }
    val semanticsModifier = if (description == null) {
        Modifier
    } else {
        Modifier.semantics { contentDescription = description }
    }
    Row(
        modifier = semanticsModifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(columns) {
            Box(
                Modifier
                    .size(squareSize)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp))
            )
        }
    }
}

private fun layoutOptionLabel(preference: MarketplaceLayoutPreference): String {
    val listingLabel = if (preference.columns == 1) "listing" else "listings"
    return "${preference.columns} $listingLabel per row"
}
