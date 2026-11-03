package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

data class ListingFilterOption(val value: String, val label: String)

@Composable
fun ListingFilterMenus(
    selectedCategory: String?,
    selectedCondition: String?,
    categories: List<ListingFilterOption>,
    conditions: List<ListingFilterOption>,
    onCategorySelected: (String?) -> Unit,
    onConditionSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
    ) {
        ListingFilterMenu(
            label = "Categories",
            selectedValue = selectedCategory,
            options = categories,
            onSelected = onCategorySelected,
            modifier = Modifier.weight(1f)
        )
        ListingFilterMenu(
            label = "Conditions",
            selectedValue = selectedCondition,
            options = conditions,
            onSelected = onConditionSelected,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ListingFilterMenu(
    label: String,
    selectedValue: String?,
    options: List<ListingFilterOption>,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.value == selectedValue }?.label
    Box(modifier = modifier) {
        BulletinButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            variant = BulletinButtonVariant.Outlined
        ) {
            Text(
                text = selectedLabel ?: "All $label",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("All $label") },
                onClick = {
                    onSelected(null)
                    expanded = false
                }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelected(option.value)
                        expanded = false
                    }
                )
            }
        }
    }
}
