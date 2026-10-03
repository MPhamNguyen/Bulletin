package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.BulletinExtras
import com.jdrms.bulletin.domain.profile.domain.model.StudentReputation

@Composable
internal fun ProfileAboutCard(bio: String?) {
    BulletinCard {
        ProfileSectionLabel("ABOUT")
        Spacer(modifier = Modifier.height(10.dp))
        val bioColor = if (bio.isNullOrBlank()) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        }
        Text(
            text = bio?.ifBlank { null }
                ?: "Senior student buying & selling tech, textbooks, and campus essentials.",
            style = MaterialTheme.typography.bodyMedium,
            color = bioColor,
            lineHeight = 22.sp
        )
    }
}

@Composable
internal fun ProfileAcademicInformationCard(
    major: String?,
    graduationDate: String?
) {
    BulletinCard {
        ProfileSectionLabel("ACADEMIC DETAILS")
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AcademicCompactBadge(
                icon = ProfileIcons.School,
                label = "Major",
                value = major?.ifBlank { null } ?: "Computer Science",
                modifier = Modifier.weight(1f)
            )
            AcademicCompactBadge(
                icon = ProfileIcons.Graduation,
                label = "Graduation",
                value = graduationDate?.ifBlank { null } ?: "Class of 2025",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AcademicCompactBadge(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
internal fun ProfileReputationCard(reputation: StudentReputation?) {
    val averageRating = reputation?.averageRating ?: 4.8
    val totalReviews = reputation?.totalReviews ?: 5

    BulletinCard {
        ProfileSectionLabel("CAMPUS REPUTATION")
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = averageRating.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Column {
                    StarRatingDisplay(
                        rating = averageRating,
                        maxStars = 5,
                        starSize = 18.dp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$totalReviews verified reviews",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = BulletinExtras.colors.successContainer,
                border = BorderStroke(1.dp, BulletinExtras.colors.success.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = ProfileIcons.Verified,
                        contentDescription = null,
                        tint = BulletinExtras.colors.success,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Top Rated",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = BulletinExtras.colors.onSuccessContainer
                    )
                }
            }
        }
    }
}

@Composable
internal fun ProfileNavigationButtons(
    onMyListingsClick: () -> Unit,
    onBookmarkedListingsClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ProfileSectionLabel("MARKETPLACE SHORTCUTS")
        ProfileActionRow(
            icon = ProfileIcons.MyListings,
            title = "My Listings",
            subtitle = "Active, pending & sold campus listings",
            onClick = onMyListingsClick
        )
        ProfileActionRow(
            icon = ProfileIcons.BookmarkedListings,
            title = "Bookmarked Listings",
            subtitle = "Saved items and favorites from marketplace",
            onClick = onBookmarkedListingsClick
        )
    }
}
