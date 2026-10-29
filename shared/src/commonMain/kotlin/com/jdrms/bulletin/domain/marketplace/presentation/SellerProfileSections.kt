package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import com.jdrms.bulletin.core.common.cropSettings
import com.jdrms.bulletin.core.common.imageRequestUrl
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.BulletinExtras
import com.jdrms.bulletin.core.designsystem.BulletinSpacing
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerProfile
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem

@Composable
internal fun SellerProfileHero(profile: MarketplaceSellerProfile) {
    BulletinCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
        ) {
            SellerAvatar(profile)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
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
            modifier = Modifier.padding(horizontal = BulletinSpacing.s, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.xs),
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
        Spacer(Modifier.height(BulletinSpacing.xs))
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
        Column(verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)) {
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
    profile.school?.takeIf(String::isNotBlank)?.let {
        com.jdrms.bulletin.core.designsystem.UniversityCard(it)
    }
}

@Composable
internal fun SellerListingCard(listing: MarketplaceItem, onListingClick: (String) -> Unit) {
    BulletinCard(modifier = Modifier.clickable { onListingClick(listing.id.value) }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SellerListingThumbnail(listing)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BulletinSpacing.xs)) {
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

@Composable
private fun SellerListingThumbnail(listing: MarketplaceItem) {
    val photoUrl = listing.photos.firstOrNull()
    val modifier = Modifier.size(72.dp).clip(MaterialTheme.shapes.small)
    if (photoUrl == null) {
        SellerListingThumbnailPlaceholder(modifier)
        return
    }
    SubcomposeAsyncImage(
        model = photoUrl.imageRequestUrl(),
        contentDescription = "${listing.title} thumbnail",
        modifier = modifier,
        contentScale = ContentScale.Crop,
        loading = { SellerListingThumbnailPlaceholder(Modifier.fillMaxSize()) },
        error = { SellerListingThumbnailPlaceholder(Modifier.fillMaxSize()) },
        success = {
            val crop = photoUrl.cropSettings()
            SubcomposeAsyncImageContent(
                Modifier.graphicsLayer {
                    scaleX = crop.scale
                    scaleY = crop.scale
                    translationX = -crop.offsetX * size.width * crop.scale
                    translationY = -crop.offsetY * size.height * crop.scale
                }
            )
        }
    )
}

@Composable
private fun SellerListingThumbnailPlaceholder(modifier: Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Image,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp)
        )
    }
}

private fun profileInitials(fullName: String): String {
    val nameParts = fullName.trim().split(Regex("\\s+")).filter(String::isNotBlank)
    if (nameParts.isEmpty()) return "ST"
    val firstInitial = nameParts.first().first().uppercaseChar()
    val lastInitial = nameParts.takeIf { it.size > 1 }?.last()?.first()?.uppercaseChar()
    return if (lastInitial == null) firstInitial.toString() else "$firstInitial$lastInitial"
}
