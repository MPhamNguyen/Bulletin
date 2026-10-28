package com.jdrms.bulletin.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jdrms.bulletin.domain.marketplace.presentation.SellerProfileListingActions
import com.jdrms.bulletin.domain.marketplace.presentation.SellerProfileScreen
import com.jdrms.bulletin.domain.marketplace.presentation.SellerProfileScreenOptions
import com.jdrms.bulletin.domain.marketplace.presentation.SellerProfileViewModel
import com.jdrms.bulletin.domain.profile.presentation.ProfileViewModel

@Composable
internal fun PublicProfilePreviewDestination(
    profileViewModel: ProfileViewModel,
    sellerProfileViewModel: SellerProfileViewModel,
    onBack: () -> Unit
) {
    val profileState by profileViewModel.uiState.collectAsState()
    val sellerProfileState by sellerProfileViewModel.uiState.collectAsState()
    val userId = profileState.profile?.id?.value
    LaunchedEffect(userId) {
        userId?.let(sellerProfileViewModel::load)
    }
    SellerProfileScreen(
        state = sellerProfileState,
        messageActions = null,
        listingActions = SellerProfileListingActions(
            onListingClick = {},
            onCategorySelected = sellerProfileViewModel::onCategorySelected,
            onConditionSelected = sellerProfileViewModel::onConditionSelected
        ),
        onBack = onBack,
        onReportUser = {},
        onBlockUser = {},
        options = SellerProfileScreenOptions(showUserActions = false)
    )
}
