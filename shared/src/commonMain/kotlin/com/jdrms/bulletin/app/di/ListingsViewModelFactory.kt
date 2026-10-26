package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.domain.listings.application.RecentListingBuyersProvider
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModel
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModelSessionDependencies

internal fun ListingsModule.createListingsViewModel(
    recentListingBuyersProvider: RecentListingBuyersProvider,
    listingChangedSignal: RefreshSignal
) = ListingsViewModel(
    createListing,
    manageListing,
    markListingSold,
    recentListingBuyersProvider,
    restoreListingToMarketplace,
    deleteListing,
    getSellerListings,
    ListingsViewModelSessionDependencies(
        currentListingSellerProvider,
        listingChangedSignal,
        manageListingImages
    )
)
