package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.BulletinOptionChip
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingCondition

@Composable
internal fun EditListingTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Edit Listing",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        BulletinButton(onClick = onBack, variant = BulletinButtonVariant.Text) {
            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ListingCategorySelectionRow(
    selectedCategory: ListingCategory?,
    onCategorySelected: (ListingCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ListingRequiredLabel("Category", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(BulletinSpacing.s))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s),
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
        ) {
            ListingCategory.entries.forEach { category ->
                BulletinOptionChip(
                    label = category.displayLabel(),
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ListingConditionSelectionRow(
    selectedCondition: ListingCondition?,
    onConditionSelected: (ListingCondition) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ListingRequiredLabel("Condition", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(BulletinSpacing.s))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s),
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
        ) {
            ListingCondition.entries.forEach { condition ->
                BulletinOptionChip(
                    label = condition.displayLabel(),
                    selected = selectedCondition == condition,
                    onClick = { onConditionSelected(condition) }
                )
            }
        }
    }
}
