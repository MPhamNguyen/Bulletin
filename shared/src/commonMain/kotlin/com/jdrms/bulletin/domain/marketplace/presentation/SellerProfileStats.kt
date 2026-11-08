package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.core.designsystem.theme.bulletinColors
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerProfile

@Composable
internal fun SellerProfileStats(
    profile: MarketplaceSellerProfile,
    soldListingsCount: Int
) {
    BulletinCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SellerProfileStat(
                "Reputation",
                formatReputationScore(profile.reputationScore),
                showStar = true
            )
            SellerProfileStat("Listings sold", soldListingsCount.toString(), showStar = false)
        }
    }
}

@Composable
private fun SellerProfileStat(
    label: String,
    value: String,
    showStar: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(BulletinSpacing.xs))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (showStar) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Reputation",
                    tint = MaterialTheme.bulletinColors.star,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
