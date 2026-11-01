package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.SectionHeader
import com.jdrms.bulletin.core.designsystem.showKeyboardOnFocusOrTap
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingCondition
import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus

@Composable
fun ListingsScreen(viewModel: ListingsViewModel) {
    val state by viewModel.uiState.collectAsState()
    val pickListingImages = rememberListingImagePicker(viewModel::addNewImages, viewModel::imageSelectionFailed)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(BulletinSpacing.l),
        verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
    ) {
        item {
            SectionHeader(
                title = "Post & Manage Listings",
                subtitle = "Create new campus listings or manage your active items"
            )
        }

        item {
            BulletinCard {
                Text(
                    text = "Create New Listing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(BulletinSpacing.s))

                OutlinedTextField(
                    value = state.newTitle,
                    onValueChange = { viewModel.onTitleChanged(it) },
                    label = { Text("Item Title") },
                    singleLine = true,
                    modifier = Modifier.showKeyboardOnFocusOrTap().fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(BulletinSpacing.s))

                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ListingCategory.entries.toTypedArray()) { category ->
                        FilterChip(
                            selected = state.newCategory == category,
                            onClick = { viewModel.onCategorySelected(category) },
                            label = { Text(category.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(BulletinSpacing.s))

                Text(
                    text = "Condition",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ListingCondition.entries.toTypedArray()) { condition ->
                        val conditionLabel = condition.name.replace("_", " ")
                        FilterChip(
                            selected = state.newCondition == condition,
                            onClick = { viewModel.onConditionSelected(condition) },
                            label = {
                                Text(
                                    text = conditionLabel,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(BulletinSpacing.s))

                CurrencyTextField(
                    digits = state.newPriceCents,
                    onDigitsChange = viewModel::onPriceChanged,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(BulletinSpacing.s))

                OutlinedTextField(
                    value = state.newDescription,
                    onValueChange = { viewModel.onDescriptionChanged(it) },
                    label = { Text("Description & Campus Pick-Up Details") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.showKeyboardOnFocusOrTap().fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(BulletinSpacing.m))
                ListingImageSelectionField(
                    photos = state.newImages.map { ListingPhotoDraft.Pending(it) },
                    onPick = pickListingImages,
                    onRemove = viewModel::removeNewImage,
                    onCropChange = viewModel::updateNewImageCrop
                )

                Spacer(modifier = Modifier.height(BulletinSpacing.m))

                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(BulletinSpacing.s))
                }

                if (state.successMessage != null) {
                    Text(
                        text = state.successMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(BulletinSpacing.s))
                }

                Button(
                    onClick = { viewModel.submitNewListing() },
                    enabled = !state.isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Post Campus Listing")
                }
            }
        }

        item {
            SectionHeader(
                title = "Your Active Listings",
                subtitle = "Manage items you have currently published"
            )
        }

        val activeListings = state.myListings.filter { it.status == ListingStatus.AVAILABLE }
        if (activeListings.isEmpty()) {
            item {
                BulletinCard {
                    Text(
                        text = "You have no active listings. Post your first item above!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(activeListings) { listing ->
            MyListingCard(
                listing = listing,
                onDelete = { viewModel.requestDeleteListing(listing) },
                onMarkSold = { viewModel.requestMarkListingSold(listing) }
            )
        }
    }

    state.pendingDeletion?.let { listing ->
        ListingDeleteConfirmationDialog(
            listing = listing,
            isDeleting = state.isDeleting,
            errorMessage = state.errorMessage,
            onDismiss = viewModel::cancelDeleteListing,
            onConfirm = viewModel::confirmDeleteListing
        )
    }
    state.pendingSold?.let { listing ->
        ListingSoldConfirmationDialog(
            state = ListingSoldConfirmationState(
                listing = listing,
                isMarkingSold = state.isMarkingSold,
                recentBuyers = state.recentSoldBuyers,
                selectedBuyerId = state.selectedBuyerId,
                soldOnAnotherPlatform = state.soldOnAnotherPlatform,
                errorMessage = state.errorMessage
            ),
            actions = ListingSoldConfirmationActions(
                onDismiss = viewModel::cancelMarkListingSold,
                onConfirm = viewModel::confirmMarkListingSold,
                onBuyerSelected = viewModel::selectSoldBuyer,
                onSoldOnAnotherPlatform = viewModel::selectSoldOnAnotherPlatform
            )
        )
    }
}

@Composable
private fun MyListingCard(
    listing: Listing,
    onDelete: () -> Unit,
    onMarkSold: () -> Unit
) {
    BulletinCard {
        ListingThumbnail(listing)
        Spacer(modifier = Modifier.height(BulletinSpacing.s))
        ActiveListingTitleAndPrice(listing)
        Spacer(modifier = Modifier.height(BulletinSpacing.xs))
        Text(
            text = "Category: ${listing.category.name} • Condition: ${listing.condition.name.replace("_", " ")}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(BulletinSpacing.s))
        Text(
            text = listing.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(BulletinSpacing.m))
        ActiveListingActions(onDelete, onMarkSold)
    }
}

@Composable
private fun ActiveListingTitleAndPrice(listing: Listing) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = listing.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = listing.price.formatted,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ActiveListingActions(onDelete: () -> Unit, onMarkSold: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        FilledTonalButton(
            onClick = onMarkSold,
            shape = MaterialTheme.shapes.small,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) { Text("Mark as sold") }
        TextButton(
            onClick = onDelete,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Delete Listing")
        }
    }
}
