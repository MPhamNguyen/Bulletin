package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.core.designsystem.theme.bulletinColors
import com.jdrms.bulletin.domain.profile.domain.model.StudentReputation

@Composable
internal fun PublicProfileReputation(reputation: StudentReputation) {
    BulletinCard {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReputationScore(reputation)
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    "${reputation.totalReviews} reviews",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ReputationScore(reputation: StudentReputation) {
    Column {
        Text(
            "Reputation Score",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(BulletinSpacing.xs))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                Icons.Filled.Star,
                contentDescription = "Rating",
                tint = MaterialTheme.bulletinColors.star,
                modifier = Modifier.size(20.dp)
            )
            val rating = if (reputation.totalReviews == 0) {
                "No ratings yet"
            } else {
                "${(reputation.averageRating * 10).toInt() / 10.0} / 5.0"
            }
            Text(rating, style = MaterialTheme.typography.titleLarge)
        }
    }
}
