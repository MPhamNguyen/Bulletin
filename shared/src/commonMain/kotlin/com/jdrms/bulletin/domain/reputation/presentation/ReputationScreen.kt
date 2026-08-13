package com.jdrms.bulletin.domain.reputation.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.SectionHeader

@Composable
fun ReputationScreen(viewModel: ReputationViewModel) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Ratings & Reputation",
                subtitle = "Trusted campus community ratings and seller reviews"
            )
        }

        val rep = state.userReputation
        if (rep != null) {
            item {
                BulletinCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Overall Rating", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "★ ${rep.averageRating} / 5.0",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text("${rep.totalReviews} Student Reviews", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.showReviewModal(true) }) {
                        Text("+ Write Review")
                    }
                }
            }

            items(rep.reviews) { review ->
                BulletinCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(review.reviewerName, fontWeight = FontWeight.Bold)
                        Text("★ ${review.rating.score}/5", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(review.comment, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        val errorMessage = state.errorMessage
        if (errorMessage != null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }

    if (state.showReviewDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showReviewModal(false) },
            title = { Text("Write Student Review") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Rating (1 to 5 stars)")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..5).forEach { score ->
                            FilterChip(
                                selected = state.newScore == score,
                                onClick = { viewModel.onScoreChanged(score) },
                                label = { Text("$score ★") }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = state.newComment,
                        onValueChange = { viewModel.onCommentChanged(it) },
                        label = { Text("Review comment") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.submitNewReview() }) {
                    Text("Submit Review")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showReviewModal(false) }) {
                    Text("Cancel")
                }
            }
        )
    }
}
