package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.theme.BulletinExtras
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

@Composable
internal fun ProfileAboutCard(
    major: String,
    university: String,
    bio: String?
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
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s),
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
        ) {
            if (major.isNotBlank()) {
                ProfileInformationChip(Icons.Outlined.School, major)
            }
            if (university.isNotBlank()) {
                ProfileInformationChip(Icons.Outlined.LocationOn, university)
            }
        }
        Spacer(modifier = Modifier.height(BulletinSpacing.m))
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
            modifier = Modifier.padding(horizontal = BulletinSpacing.m, vertical = 7.dp),
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
    activeListings: String,
    itemsSold: Int?,
    rating: Double?,
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
                value = activeListings,
                label = "Active listings",
                icon = Icons.Outlined.Storefront,
                modifier = Modifier.weight(1f)
            )
            ActivityDivider()
            ActivityStat(
                value = formatProfileStat(itemsSold),
                label = "Items sold",
                icon = Icons.Outlined.CheckCircle,
                modifier = Modifier.weight(1f)
            )
            ActivityDivider()
            ActivityStat(
                value = formatProfileStat(rating),
                label = "Reputation",
                icon = Icons.Filled.Star,
                iconTint = BulletinExtras.colors.star,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

internal fun formatProfileStat(value: Number?): String = value?.toString() ?: "—"

@Composable
private fun ActivityStat(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = modifier.padding(horizontal = BulletinSpacing.xs, vertical = BulletinSpacing.l),
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
            .padding(vertical = BulletinSpacing.m)
            .width(1.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    ) {}
}
