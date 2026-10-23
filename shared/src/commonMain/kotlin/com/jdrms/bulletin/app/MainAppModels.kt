package com.jdrms.bulletin.app

import com.jdrms.bulletin.app.theme.ThemeViewModel
import com.jdrms.bulletin.domain.home.presentation.HomeViewModel
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.ListingReportViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceUserBlockViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceUserReportViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.SellerProfileViewModel
import com.jdrms.bulletin.domain.messages.presentation.MessagesViewModel
import com.jdrms.bulletin.domain.profile.presentation.ProfileViewModel

internal class ProfilePresentation(
    val viewModel: ProfileViewModel,
    val themeViewModel: ThemeViewModel?,
    val onSignOut: () -> Unit
)

internal class MainScaffoldModels(
    val home: HomeViewModel,
    val marketplace: MarketplaceViewModel,
    val sellerProfile: SellerProfileViewModel,
    val marketplaceUserReport: MarketplaceUserReportViewModel,
    val listingReport: ListingReportViewModel,
    val marketplaceUserBlock: MarketplaceUserBlockViewModel,
    val listings: ListingsViewModel,
    val messages: MessagesViewModel
)
