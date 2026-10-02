package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        Spacer(modifier = Modifier.height(8.dp))
        val bioColor = if (bio.isNullOrBlank()) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        }
        Text(
            text = bio?.ifBlank { null }
                ?: "No bio added yet. Tap Edit to tell fellow students about yourself!",
            style = MaterialTheme.typography.bodyMedium,
            color = bioColor,
            lineHeight = 20.sp
        )
    }
}

@Composable
internal fun ProfileAcademicInformationCard(university: String?, major: String?, email: String?) {
    BulletinCard {
        ProfileSectionLabel("ACADEMIC INFORMATION")
        Spacer(modifier = Modifier.height(12.dp))
        ProfileInfoRow("School", university?.ifBlank { null } ?: "CSU Long Beach")
        ProfileDivider()
        ProfileInfoRow("Major", major?.ifBlank { null } ?: "Undeclared")
        ProfileDivider()
        ProfileInfoRow("Email", email ?: "No email linked")
    }
}

@Composable
private fun ProfileDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 10.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
    )
}

@Composable
internal fun ProfileReputationCard(reputation: StudentReputation?) {
    BulletinCard {
        ProfileSectionLabel("CAMPUS REPUTATION")
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ReputationMetric(
                label = "Average Rating",
                value = (reputation?.averageRating ?: 5.0).toString(),
                showStar = true,
                modifier = Modifier.weight(1f)
            )
            ReputationMetric(
                label = "Feedback",
                value = "${reputation?.totalReviews ?: 0} reviews",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ReputationMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    showStar: Boolean = false
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                MaterialTheme.shapes.medium
            )
            .padding(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showStar) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = BulletinExtras.colors.star,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (showStar) FontWeight.Bold else FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
