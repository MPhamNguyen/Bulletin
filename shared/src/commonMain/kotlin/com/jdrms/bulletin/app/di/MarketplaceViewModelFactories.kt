package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.domain.marketplace.presentation.ListingReportViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceBookmarkDependencies
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceUserBlockViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceUserReportViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.SellerProfileViewModel

internal fun MarketplaceModule.createMarketplaceViewModel(listingChangedSignal: RefreshSignal) = MarketplaceViewModel(
    searchMarketplace,
    MarketplaceBookmarkDependencies(
        add = bookmarkMarketplaceListing,
        remove = removeMarketplaceListingBookmark,
        get = getMarketplaceListingBookmarks
    ),
    viewMarketplaceListing,
    submitMarketplaceReview,
    listingChangedSignal,
    currentUserIdProvider,
    marketplaceLayoutPreferenceStore
)

internal fun MarketplaceModule.createMarketplaceUserReportViewModel() =
    MarketplaceUserReportViewModel(reportMarketplaceUser)

internal fun MarketplaceModule.createMarketplaceUserBlockViewModel(listingChangedSignal: RefreshSignal) =
    MarketplaceUserBlockViewModel(blockMarketplaceUser, listingChangedSignal)

internal fun MarketplaceModule.createSellerProfileViewModel() = SellerProfileViewModel(viewMarketplaceSellerProfile)

internal fun MarketplaceModule.createListingReportViewModel() =
    ListingReportViewModel(reportMarketplaceListing)
