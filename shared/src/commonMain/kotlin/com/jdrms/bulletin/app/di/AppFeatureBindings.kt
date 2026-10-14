package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.app.integration.ProfileMarketplaceSellerNameProvider
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.RecentListingBuyer
import com.jdrms.bulletin.domain.listings.application.RecentListingBuyersProvider
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerSalesProvider
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceSellerProfile
import com.jdrms.bulletin.domain.messages.application.GetConversations
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository

internal fun createRecentListingBuyersProvider(
    getConversations: GetConversations
): RecentListingBuyersProvider = RecentListingBuyersProvider { listingId, sellerId ->
    when (val result = getConversations()) {
        is Result.Success -> {
            result.data.conversations
                .asSequence()
                .filter { it.listingId?.value == listingId.value }
                .flatMap { conversation -> conversation.participants.asSequence() }
                .filter { it.id.value != sellerId }
                .distinctBy { it.id }
                .map { RecentListingBuyer(it.id.value, it.displayName) }
                .toList()
        }
        is Result.Error -> emptyList()
    }
}

internal fun createMarketplaceSellerProfileView(
    sellerProfileProvider: ProfileMarketplaceSellerNameProvider,
    listingSource: MarketplaceListingSource,
    sellerSalesProvider: MarketplaceSellerSalesProvider
): ViewMarketplaceSellerProfile =
    ViewMarketplaceSellerProfile(sellerProfileProvider, listingSource, sellerSalesProvider)

internal fun createCurrentUserIdProvider(
    authRepository: AuthRepository
): suspend () -> String? = {
    authRepository.getCurrentUserId().let { result ->
        when (result) {
            is Result.Success -> result.data?.value
            is Result.Error -> null
        }
    }
}
