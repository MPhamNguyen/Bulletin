package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.BulletinExtras
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerProfile
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem

@Composable
internal fun SellerProfileHero(profile: MarketplaceSellerProfile) {
    BulletinCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SellerAvatar(profile)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = profile.name ?: "Student Seller",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (profile.isVerified) VerifiedBadge()
            }
            val schoolInfo = listOfNotNull(
                profile.major?.takeIf(String::isNotBlank),
                profile.school?.takeIf(String::isNotBlank)
            ).joinToString(" • ")
            if (schoolInfo.isNotBlank()) {
                Text(
                    text = schoolInfo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SellerAvatar(profile: MarketplaceSellerProfile) {
    if (profile.avatarUrl != null) {
        AsyncImage(
            model = profile.avatarUrl,
            contentDescription = "${profile.name} avatar",
            modifier = Modifier.size(88.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = profileInitials(profile.name ?: "Student"),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun VerifiedBadge() {
    Surface(
        shape = CircleShape,
        color = BulletinExtras.colors.successContainer,
        contentColor = BulletinExtras.colors.success
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(12.dp))
            Text("Verified", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun SellerProfileStats(profile: MarketplaceSellerProfile, soldListingsCount: Int) {
    BulletinCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SellerStat("Reputation", formatReputationScore(profile.reputationScore), showStar = true)
            SellerStat("Listings sold", soldListingsCount.toString(), showStar = false)
        }
    }
}

@Composable
private fun SellerStat(label: String, value: String, showStar: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (showStar) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Reputation",
                    tint = BulletinExtras.colors.star,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
internal fun SellerProfileAbout(profile: MarketplaceSellerProfile) {
    BulletinCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("About the Seller", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = profile.bio?.takeIf(String::isNotBlank) ?: "This seller hasn't added a bio yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun SellerProfileVerification(profile: MarketplaceSellerProfile) {
    BulletinCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    text = profile.school?.takeIf(String::isNotBlank) ?: "Campus",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Verified student with official campus email",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun SellerListingCard(listing: MarketplaceItem, onListingClick: (String) -> Unit) {
    BulletinCard(modifier = Modifier.clickable { onListingClick(listing.id.value) }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(listing.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = listing.category.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = listing.price.formatted,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun profileInitials(fullName: String): String {
    val nameParts = fullName.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    if (nameParts.isEmpty()) return "ST"
    val firstInitial = nameParts.first().first().uppercaseChar()
    val lastInitial = nameParts.takeIf { it.size > 1 }?.last()?.first()?.uppercaseChar()
    return if (lastInitial == null) firstInitial.toString() else "$firstInitial$lastInitial"
}
