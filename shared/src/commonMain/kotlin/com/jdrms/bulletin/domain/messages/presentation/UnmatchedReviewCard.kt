package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.domain.messages.application.PendingConversationReview

/** Independent review entry point: hidden chats and their history stay hidden. */
@Composable
internal fun UnmatchedReviewCard(
    request: PendingConversationReview,
    state: MessagesUiState,
    interaction: ConversationReviewInteraction
) {
    var expanded by remember(state.viewerId, request) { mutableStateOf(false) }
    val reviews by interaction.state.collectAsState()
    BulletinCard {
        Text(
            state.listingPreviews[request.listingId.value]?.title ?: "Review your purchase",
            style = MaterialTheme.typography.titleSmall
        )
        BulletinButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Close review" else "Leave a review")
        }
        if (expanded) {
            BuyerSaleNotice(
                "the seller",
                reviews[request.listingId.value] ?: ConversationReviewState.Ready,
                { rating, comment -> interaction.submitPending(request, rating, comment) }
            )
        }
    }
}
