package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.ReviewSubmissionDialog
import com.jdrms.bulletin.core.designsystem.dismissKeyboardOnTapOutside
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

@Composable
fun ListingDetailsPage(
    state: MarketplaceDetailSheetState,
    actions: MarketplaceDetailSheetActions,
    photoModifier: Modifier = Modifier
) {
    val listing = state.listing
    val isLoading = state.isLoading
    val errorMessage = state.errorMessage
    val onDismiss = actions.onDismiss
    val onRetry = actions.onRetry
    val onSellerClick = actions.onSellerClick
    val onDismissReview = actions.onDismissReview
    val onSubmitReview = actions.onSubmitReview
    BackHandler(onBack = onDismiss)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = BulletinSpacing.s,
                        vertical = BulletinSpacing.xs
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Listing Details", style = MaterialTheme.typography.titleLarge)
            }
            Column(
                modifier = Modifier.dismissKeyboardOnTapOutside()
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(BulletinSpacing.l)
            ) {
                when {
                    listing == null && isLoading && state.preview != null -> MarketplaceDetailPreview(
                        state.preview,
                        photoModifier
                    )
                    listing == null && isLoading -> ListingDetailsLoading(state.previewPhotoUrl, photoModifier)

                    listing == null && errorMessage != null -> ListingDetailsError(errorMessage, onRetry)

                    listing != null -> {
                        // Photos section
                        Box(photoModifier) { ListingPhotosSection(photos = listing.photos) }

                        MarketplaceListingHeader(listing)

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Seller Information & Reputation Score Card
                        SellerInfoCard(
                            seller = SellerInfoCardState(
                                name = listing.sellerName,
                                school = listing.sellerSchool,
                                avatarUrl = listing.sellerAvatarUrl,
                                reputationScore = listing.sellerReputationScore,
                                major = listing.sellerMajor,
                                reviewCount = listing.sellerReviewCount,
                                isVerified = listing.sellerIsVerified
                            ),
                            onClick = { onSellerClick(listing.sellerId) }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Description Section
                        MarketplaceListingDescription(listing)

                        MarketplaceItemDetails(listing)
                    }
                }
                ListingInteractionSection(state, actions)
            }
            if (state.isReviewDialogOpen) {
                ReviewSubmissionDialog(
                    isSubmitting = state.isSubmittingReview,
                    errorMessage = state.reviewErrorMessage,
                    onDismiss = onDismissReview,
                    onSubmit = onSubmitReview
                )
            }
        }
    }
}
