package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterest

@Composable
internal fun MarketplaceInterestCategoryFilters(
    interests: List<MarketplaceInterest>,
    onEdit: (MarketplaceCategory) -> Unit
) {
    interests.sortedBy { it.category.ordinal }.forEach { interest ->
        BulletinCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = interest.category.displayName(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium
                )
                BulletinButton(
                    onClick = { onEdit(interest.category) },
                    variant = BulletinButtonVariant.Text
                ) {
                    Text("Filters")
                }
            }
            Text(
                text = interest.filterSummary(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun MarketplaceInterest.filterSummary(): String = when (activeFilterCount) {
    0 -> "No filters selected"
    1 -> "1 filter selected"
    else -> "$activeFilterCount filters selected"
}
