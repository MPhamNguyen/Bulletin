package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.common.ImageCropSettings
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory

internal data class CreateListingPhotosActions(
    val onPickPhotos: () -> Unit,
    val onRemovePhoto: (Int) -> Unit,
    val onCropPhoto: (Int, ImageCropSettings) -> Unit,
    val onTitleChange: (String) -> Unit,
    val onCategorySelected: (ListingCategory) -> Unit
)

@Composable
internal fun CreateListingPhotosStep(
    state: ListingsUiState,
    actions: CreateListingPhotosActions
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StepIntro("What are you selling?", "Add at least one photo. The first one becomes the cover.")
        Text(
            text = "Fields marked * are required.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ListingRequiredLabel("Listing photos", style = MaterialTheme.typography.titleSmall)
        ListingImageSelectionField(
            photos = state.newImages.map { ListingPhotoDraft.Pending(it) },
            onPick = actions.onPickPhotos,
            onRemove = actions.onRemovePhoto,
            onCropChange = actions.onCropPhoto
        )
        OutlinedTextField(
            value = state.newTitle,
            onValueChange = actions.onTitleChange,
            label = { ListingRequiredLabel("Title") },
            placeholder = { Text("e.g. Calculus textbook, 8th edition") },
            singleLine = true,
            colors = BulletinTextFieldDefaults.colors(),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        )
        ListingRequiredLabel("Category", style = MaterialTheme.typography.titleSmall)
        CategoryGrid(selected = state.newCategory, onSelected = actions.onCategorySelected)
    }
}

@Composable
private fun CategoryGrid(selected: ListingCategory, onSelected: (ListingCategory) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ListingCategory.entries.chunked(CATEGORY_COLUMNS).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { category ->
                    CategoryTile(category, category == selected, onSelected, Modifier.weight(1f))
                }
                repeat(CATEGORY_COLUMNS - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CategoryTile(
    category: ListingCategory,
    selected: Boolean,
    onSelected: (ListingCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        selected = selected,
        onClick = { onSelected(category) },
        shape = MaterialTheme.shapes.medium,
        color = if (selected) scheme.primaryContainer else scheme.surfaceVariant,
        border = if (selected) BorderStroke(1.5.dp, scheme.primary) else null,
        modifier = modifier.heightIn(min = 72.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            Icon(
                imageVector = category.icon(),
                contentDescription = null,
                tint = if (selected) scheme.primary else scheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = category.displayLabel(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = scheme.onSurface
            )
        }
    }
}

private const val CATEGORY_COLUMNS = 3
