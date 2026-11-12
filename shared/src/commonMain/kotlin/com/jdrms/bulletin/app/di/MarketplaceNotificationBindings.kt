package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceInterestIdentity
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceInterestMatchLoader
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceInterestNotificationCoordinator

internal fun MarketplaceModule.createInterestNotificationCoordinator(
    identity: MarketplaceInterestIdentity
) = MarketplaceInterestNotificationCoordinator(
    identity = identity,
    listingObserver = marketplaceListingInsertObserver,
    listingEvents = interestListingEvents,
    matchLoader = MarketplaceInterestMatchLoader(searchMarketplace, ::invalidateReads),
    notifier = interestNotifier
)
