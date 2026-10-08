package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.BulletinExtras

@Composable
internal fun ProfileAboutCard(
    major: String,
    university: String,
    bio: String?,
    onEditProfileClick: () -> Unit
) {
    BulletinCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "About",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onEditProfileClick) {
                Text("Edit", style = MaterialTheme.typography.labelMedium)
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (major.isNotBlank()) {
                ProfileInformationChip(Icons.Outlined.School, major)
            }
            if (university.isNotBlank()) {
                ProfileInformationChip(Icons.Outlined.LocationOn, university)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = bio?.ifBlank { null }
                ?: "Senior student buying & selling tech, textbooks, and campus essentials.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileInformationChip(icon: ImageVector, text: String) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
internal fun MarketplaceActivityStatsCard(
    activeListings: Int,
    itemsSold: Int,
    rating: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActivityStat(
                value = activeListings.toString(),
                label = "Active listings",
                icon = Icons.Outlined.Storefront,
                modifier = Modifier.weight(1f)
            )
            ActivityDivider()
            ActivityStat(
                value = itemsSold.toString(),
                label = "Items sold",
                icon = Icons.Outlined.CheckCircle,
                modifier = Modifier.weight(1f)
            )
            ActivityDivider()
            ActivityStat(
                value = rating.toString(),
                label = "Reputation",
                icon = Icons.Filled.Star,
                iconTint = BulletinExtras.colors.star,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ActivityStat(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun ActivityDivider() {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = 12.dp)
            .width(1.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    ) {}
}

@Composable
internal fun ProfileMarketplaceCard(
    activeListings: Int,
    onMyListingsClick: () -> Unit,
    onBookmarkedListingsClick: () -> Unit,
    onCreateListingClick: () -> Unit
) {
    BulletinCard {
        Text(
            text = "Marketplace",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        MarketplaceActionRow(
            icon = Icons.Outlined.GridView,
            title = "My listings",
            badge = activeListings.toString(),
            onClick = onMyListingsClick
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        MarketplaceActionRow(
            icon = Icons.Outlined.BookmarkBorder,
            title = "Bookmarked listings",
            onClick = onBookmarkedListingsClick
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        MarketplaceActionRow(
            icon = Icons.Outlined.Add,
            title = "Create a listing",
            onClick = onCreateListingClick
        )
    }
}

@Composable
private fun MarketplaceActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    badge: String? = null
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (badge != null) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .widthIn(min = 12.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
