package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.app.application.GetVisibleMarketplaceListings
import com.jdrms.bulletin.app.integration.AuthMarketplaceReportIdentity
import com.jdrms.bulletin.app.integration.CompositeMarketplaceListingSource
import com.jdrms.bulletin.app.integration.ListingsMarketplaceListingSource
import com.jdrms.bulletin.app.integration.ListingsMarketplaceSellerSalesProvider
import com.jdrms.bulletin.app.integration.ProfileMarketplaceSellerNameProvider
import com.jdrms.bulletin.domain.marketplace.application.BlockMarketplaceUser
import com.jdrms.bulletin.domain.marketplace.application.BookmarkMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.application.GetMarketplaceListingBookmarks
import com.jdrms.bulletin.domain.marketplace.application.GetMarketplaceReportedListings
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceLayoutPreferenceStore
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceRepositoryListingSource
import com.jdrms.bulletin.domain.marketplace.application.RemoveMarketplaceListingBookmark
import com.jdrms.bulletin.domain.marketplace.application.ReportMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.application.ReportMarketplaceUser
import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.application.SubmitMarketplaceReview
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.domain.repository.ListingReportRepository
import com.jdrms.bulletin.domain.marketplace.domain.repository.MarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryListingReportRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.MarketplaceReadCache
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.SupabaseListingReportRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.SupabaseMarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.SupabaseMarketplaceRepository

internal class MarketplaceModule(
    private val infrastructure: InfrastructureModule,
    private val profile: ProfileModule,
    private val listings: ListingsModule,
    internal val marketplaceLayoutPreferenceStore: MarketplaceLayoutPreferenceStore
) {
    val getVisibleMarketplaceListings by lazy {
        GetVisibleMarketplaceListings(
            listingSource = marketplaceListingSource,
            blockedUserProvider = marketplaceBlockedUserProvider,
            reportedListingProvider = marketplaceReportedListingProvider
        )
    }
    internal val marketplaceRepository: MarketplaceRepository by lazy {
        infrastructure.backend(
            supabase = { SupabaseMarketplaceRepository(it, readCache = marketplaceReadCache) },
            inMemory = { InMemoryMarketplaceRepository() }
        )
    }
    val marketplaceListingSource: MarketplaceListingSource by lazy {
        infrastructure.backend(
            supabase = { SupabaseMarketplaceListingSource(it, marketplaceReadCache) },
            inMemory = {
                CompositeMarketplaceListingSource(
                    ListingsMarketplaceListingSource(listings.listingsRepository),
                    MarketplaceRepositoryListingSource(marketplaceRepository)
                )
            }
        )
    }
    private val marketplaceBlockedUserProvider get() = profile.marketplaceBlockedUserProvider
    val searchMarketplace by lazy {
        SearchMarketplace(
            repository = marketplaceRepository,
            listingSource = marketplaceListingSource,
            blockedUserProvider = marketplaceBlockedUserProvider,
            reportedListingProvider = marketplaceReportedListingProvider,
            getVisibleListings = getVisibleMarketplaceListings
        )
    }
    val bookmarkMarketplaceListing by lazy { BookmarkMarketplaceListing(marketplaceRepository) }
    val getMarketplaceListingBookmarks by lazy { GetMarketplaceListingBookmarks(marketplaceRepository) }
    val removeMarketplaceListingBookmark by lazy { RemoveMarketplaceListingBookmark(marketplaceRepository) }
    private val marketplaceSellerProfileProvider by lazy {
        ProfileMarketplaceSellerNameProvider(profile.profileRepository)
    }
    private val listingReportRepository: ListingReportRepository by lazy {
        infrastructure.backend(
            supabase = ::SupabaseListingReportRepository,
            inMemory = ::InMemoryListingReportRepository
        )
    }
    private val marketplaceReportedListingProvider by lazy {
        GetMarketplaceReportedListings(reportIdentity, listingReportRepository)
    }
    val reportMarketplaceListing by lazy {
        ReportMarketplaceListing(reportIdentity, listingReportRepository)
    }
    val viewMarketplaceListing by lazy {
        ViewMarketplaceListing(
            repository = marketplaceRepository,
            sellerProfileProvider = marketplaceSellerProfileProvider,
            blockedUserProvider = marketplaceBlockedUserProvider,
            reportedListingProvider = marketplaceReportedListingProvider
        )
    }
    val submitMarketplaceReview by lazy { SubmitMarketplaceReview(marketplaceRepository) }
    internal val viewMarketplaceSellerProfile by lazy {
        createMarketplaceSellerProfileView(
            marketplaceSellerProfileProvider,
            marketplaceListingSource,
            ListingsMarketplaceSellerSalesProvider(listings.listingsRepository)
        )
    }
    internal val reportMarketplaceUser by lazy {
        ReportMarketplaceUser(profile.marketplaceUserReporter)
    }
    internal val blockMarketplaceUser by lazy { BlockMarketplaceUser(profile.marketplaceUserBlocker) }
    private val marketplaceReadCache by lazy { MarketplaceReadCache() }
    internal val currentUserIdProvider by lazy { createCurrentUserIdProvider(profile.authRepository) }
    private val reportIdentity by lazy { AuthMarketplaceReportIdentity(profile.authRepository) }

    suspend fun invalidateReads() = marketplaceReadCache.invalidateAll()
}
