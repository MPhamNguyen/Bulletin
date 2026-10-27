package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.app.integration.MarketplaceHomeRepository
import com.jdrms.bulletin.app.integration.MarketplaceHomeSaveAdapter
import com.jdrms.bulletin.app.integration.MessagesHomeContactSellerAdapter
import com.jdrms.bulletin.domain.home.application.GetPersonalizedFeed
import com.jdrms.bulletin.domain.home.application.RecordSwipeDecision
import com.jdrms.bulletin.domain.home.application.SendQuickMessage
import com.jdrms.bulletin.domain.home.application.UpdateUserPreferences
import com.jdrms.bulletin.domain.home.infrastructure.repository.InMemorySwipeDecisionRepository

internal class HomeModule(
    private val marketplace: MarketplaceModule,
    private val messages: MessagesModule
) {
    internal val homeRepository by lazy { MarketplaceHomeRepository(marketplace.getVisibleMarketplaceListings) }
    val getPersonalizedFeed by lazy { GetPersonalizedFeed(homeRepository) }
    val updateUserPreferences by lazy { UpdateUserPreferences(homeRepository) }

    // Home decisions intentionally live for one app process; they are not synced to the backend.
    internal val swipeDecisionRepository by lazy { InMemorySwipeDecisionRepository() }
    val homeSaveListing by lazy { MarketplaceHomeSaveAdapter(marketplace.bookmarkMarketplaceListing) }
    val homeContactSeller by lazy {
        MessagesHomeContactSellerAdapter(
            marketplace.viewMarketplaceListing,
            marketplace.getVisibleMarketplaceListings,
            messages.messageSeller
        )
    }
    val recordSwipeDecision by lazy { RecordSwipeDecision(swipeDecisionRepository) }
    val sendQuickMessage by lazy { SendQuickMessage(homeContactSeller) }
}
