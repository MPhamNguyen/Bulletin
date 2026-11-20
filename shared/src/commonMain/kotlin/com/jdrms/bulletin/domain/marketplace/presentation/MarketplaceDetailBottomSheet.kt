package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.ReviewSubmissionDialog
import com.jdrms.bulletin.core.designsystem.dismissKeyboardOnTapOutside
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSheetDefaults
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceDetailBottomSheet(
    state: MarketplaceDetailSheetState,
    actions: MarketplaceDetailSheetActions
) {
    val listing = state.listing
    val isLoading = state.isLoading
    val errorMessage = state.errorMessage
    val onDismiss = actions.onDismiss
    val onRetry = actions.onRetry
    val onSellerClick = actions.onSellerClick
    val onDismissReview = actions.onDismissReview
    val onSubmitReview = actions.onSubmitReview
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = BulletinSheetDefaults.shape
    ) {
        Column(
            modifier = Modifier.dismissKeyboardOnTapOutside()
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.l)
        ) {
            when {
                listing == null && isLoading && state.preview != null -> MarketplaceDetailPreview(
                    state.preview
                )
                listing == null && isLoading -> ListingDetailSkeleton()

                listing == null && errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = BulletinSpacing.xl),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(BulletinSpacing.xs))
                            BulletinButton(
                                onClick = onRetry,
                                variant = BulletinButtonVariant.Primary
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }

                listing != null -> {
                    // Photos section
                    ListingPhotosSection(photos = listing.photos)

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
