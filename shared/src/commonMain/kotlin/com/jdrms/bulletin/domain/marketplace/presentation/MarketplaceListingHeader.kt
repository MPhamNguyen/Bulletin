package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.skeleton
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing

@Composable
internal fun MarketplaceListingHeader(listing: Listing) {
    MarketplaceListingHeader(listing.title, listing.price.formatted, listing.category.name, listing.condition)
}

@Composable
internal fun MarketplaceListingHeader(
    title: String?,
    price: String?,
    category: String?,
    condition: String?,
    isLoading: Boolean = false
) {
    // Title & Price Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = title ?: "Listing title",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).skeleton(isLoading && title == null)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = price ?: "$00.00",
            modifier = Modifier.skeleton(isLoading && price == null, color = MaterialTheme.colorScheme.onSurface),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    // Metadata Badges (Category & Condition)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.skeleton(isLoading && category == null),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Text(
                text = category ?: "CATEGORY",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Surface(
            modifier = Modifier.skeleton(
                isLoading && condition == null,
                color = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            val conditionText = (condition ?: "CONDITION").replace("_", " ")
            Text(
                text = conditionText,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
