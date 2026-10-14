package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.jdrms.bulletin.core.designsystem.BulletinExtras
import com.jdrms.bulletin.domain.profile.domain.model.StudentReputation

@Composable
internal fun ProfileReviewsSection(reputation: StudentReputation) {
    if (reputation.reviews.isEmpty()) return

    BulletinCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Reviews", style = MaterialTheme.typography.titleMedium)
            reputation.reviews.forEach { review ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(review.reviewerName, style = MaterialTheme.typography.labelLarge)
                        Row {
                            (1..5).forEach { value ->
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = if (value <= review.rating.score) {
                                        BulletinExtras.colors.star
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    modifier = Modifier.padding(end = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = review.comment,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
