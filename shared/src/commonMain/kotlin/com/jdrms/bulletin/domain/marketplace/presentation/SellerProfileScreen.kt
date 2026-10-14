package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.MessageSellerQuickAction
import com.jdrms.bulletin.core.designsystem.MessageSellerQuickActionActions

@Composable
fun SellerProfileScreen(
    state: SellerProfileUiState,
    messageActions: MessageSellerQuickActionActions?,
    onListingClick: (String) -> Unit,
    onBack: () -> Unit,
    onReportUser: () -> Unit,
    onBlockUser: () -> Unit,
    blockState: SellerProfileBlockState = SellerProfileBlockState()
) {
    var showBlockConfirmation by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SellerProfileTopBar(
            onBack = onBack,
            onReportUser = onReportUser,
            onBlockUser = { showBlockConfirmation = true }
        )
        when {
            state.isLoading -> SellerProfileLoading()
            state.errorMessage != null -> SellerProfileError(state.errorMessage)
            state.profile != null -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { SellerProfileHero(state.profile) }
                item { SellerProfileStats(state.profile, state.soldListingsCount) }
                messageActions?.let { actions -> item { MessageSellerQuickAction(actions) } }
                item { SellerProfileAbout(state.profile) }
                item { SellerProfileVerification(state.profile) }
                item {
                    Text(
                        text = "Marketplace listings",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                if (state.listings.isEmpty()) {
                    item {
                        BulletinCard {
                            Text(
                                text = "This seller has no active listings.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(state.listings, key = { it.id.value }) { listing ->
                        SellerListingCard(listing, onListingClick)
                    }
                }
            }
        }
    }
    if (showBlockConfirmation) {
        BlockUserConfirmationDialog(
            isBlocking = blockState.isBlocking,
            onConfirm = onBlockUser,
            onDismiss = { if (!blockState.isBlocking) showBlockConfirmation = false }
        )
    }
    BlockUserErrorMessage(blockState.errorMessage)
}

@Composable
private fun SellerProfileLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SellerProfileError(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        BulletinCard {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
