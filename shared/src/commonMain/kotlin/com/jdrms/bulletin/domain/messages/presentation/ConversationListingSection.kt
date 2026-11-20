package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.messages.application.ConversationListingPreview
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant

@Composable
internal fun ConversationListingSection(
    state: MessagesUiState,
    viewModel: MessagesViewModel,
    conversation: Conversation,
    counterpart: ConversationParticipant?,
    navigation: ConversationNavigation
) {
    conversation.listingId?.let { listingId ->
        val preview = state.listingPreviews[listingId.value]
        val isSeller = isSeller(state, listingId.value)
        val sellerCanSell = isSeller && preview?.isSold != true
        val sellerCanUndo = isSeller && preview?.isSold == true
        ConversationListingCard(
            preview,
            onClick = navigation.onViewListing?.let { onViewListing ->
                {
                    onViewListing(listingId.value)
                }
            },
            modifier = Modifier.padding(horizontal = BulletinSpacing.m, vertical = BulletinSpacing.s),
            sellAction = counterpart?.takeIf { sellerCanSell }?.let { buyer ->
                ListingSellAction(buyer.displayName, state.isUpdatingListingSale) {
                    viewModel.markListingSold(listingId.value, buyer.id.value)
                }
            },
            soldAction = counterpart?.takeIf { sellerCanUndo }?.let { buyer ->
                ListingSoldAction(
                    soldLabel(preview, buyer.id.value, buyer.displayName),
                    state.isUpdatingListingSale
                ) { viewModel.undoListingSale(listingId.value) }
            }
        )
    }
}

private fun isSeller(state: MessagesUiState, listingId: String): Boolean =
    state.viewerId?.value != null && state.listingPreviews[listingId]?.sellerId == state.viewerId.value

private fun soldLabel(preview: ConversationListingPreview?, counterpartId: String, counterpartName: String): String =
    when (preview?.soldToBuyerId) {
        null -> "Marked as sold"
        counterpartId -> "Sold to $counterpartName"
        else -> "Sold to another buyer"
    }

@Composable
internal fun ListingSaleError(message: String) {
    Text(
        message,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = BulletinSpacing.gutter)
    )
}
