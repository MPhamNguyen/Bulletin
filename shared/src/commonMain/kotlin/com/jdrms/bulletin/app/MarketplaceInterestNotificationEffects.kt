package com.jdrms.bulletin.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jdrms.bulletin.app.di.MarketplaceModule
import com.jdrms.bulletin.app.navigation.MutableMainNavigationState
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterestOwnerId
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceViewModel
import com.jdrms.bulletin.domain.profile.presentation.ProfileViewModel

@Composable
internal fun MarketplaceInterestNotificationEffects(
    marketplace: MarketplaceModule,
    navigation: MutableMainNavigationState,
    marketplaceViewModel: MarketplaceViewModel,
    profileViewModel: ProfileViewModel
) {
    val request by marketplace.notificationRequests.pending.collectAsState()
    val profile by profileViewModel.uiState.collectAsState()
    val ownerId = profile.profile?.id?.value?.let(::MarketplaceInterestOwnerId)
    LaunchedEffect(request, ownerId) {
        val listingId = marketplace.notificationRequests.take(ownerId) ?: return@LaunchedEffect
        marketplaceViewModel.onListingClicked(listingId.value)
        navigation.openListing(listingId.value)
    }
}
