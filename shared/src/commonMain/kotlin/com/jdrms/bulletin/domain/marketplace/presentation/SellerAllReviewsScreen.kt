package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerProfile

@Composable
internal fun SellerAllReviewsScreen(
    profile: MarketplaceSellerProfile,
    onBack: () -> Unit,
    onListingClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        SellerProfileTopBar(
            onBack = onBack,
            onReportUser = {},
            onBlockUser = {},
            showUserActions = false,
            title = "All reviews",
            backContentDescription = "Back to seller profile"
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = BulletinSpacing.m),
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
        ) {
            item {
                Text(
                    "Reviews for ${profile.name ?: "this seller"}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (profile.reviews.isEmpty()) {
                item {
                    BulletinCard {
                        Text(
                            "This seller has no reviews yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(
                    profile.reviews.sortedByDescending { it.createdAtMillis },
                    key = { it.id }
                ) { review ->
                    SellerReviewCard(review, onListingClick)
                }
            }
        }
    }
}
