package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.app.application.GetVisibleMarketplaceListings
import com.jdrms.bulletin.app.integration.AuthMarketplaceReportIdentity
import com.jdrms.bulletin.app.integration.CompositeMarketplaceListingSource
import com.jdrms.bulletin.app.integration.ListingsMarketplaceListingSource
import com.jdrms.bulletin.app.integration.ListingsMarketplaceSellerSalesProvider
import com.jdrms.bulletin.app.integration.PreferenceAwareReportedListingProvider
import com.jdrms.bulletin.app.integration.ProfileMarketplaceSellerNameProvider
import com.jdrms.bulletin.domain.marketplace.application.BlockMarketplaceUser
import com.jdrms.bulletin.domain.marketplace.application.BookmarkMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.application.EmptyMarketplaceListingInsertObserver
import com.jdrms.bulletin.domain.marketplace.application.GetMarketplaceListingBookmarks
import com.jdrms.bulletin.domain.marketplace.application.GetMarketplaceReportedListings
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceInterestNotifier
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceInterestPreferenceStore
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingInsertObserver
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
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryReviewRequestRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.MarketplaceReadCache
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.PollingMarketplaceListingInsertObserver
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.SupabaseListingReportRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.SupabaseMarketplaceInterestPreferenceStore
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.SupabaseMarketplaceListingIdSnapshotSource
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.SupabaseMarketplaceListingInsertObserver
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.SupabaseMarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.SupabaseMarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.SupabaseReviewRequestRepository
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceInterestListingEvents
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceInterestNotificationRequests

internal class MarketplaceModule(
    private val infrastructure: InfrastructureModule,
    private val profile: ProfileModule,
    private val listings: ListingsModule,
    private val preferences: MarketplacePreferenceStores,
    internal val interestNotifier: MarketplaceInterestNotifier
) {
    internal val notificationRequests by lazy { MarketplaceInterestNotificationRequests() }
    internal val interestListingEvents by lazy { MarketplaceInterestListingEvents() }
    internal val marketplaceLayoutPreferenceStore get() = preferences.layout
    internal val marketplaceInterestPreferenceStore: MarketplaceInterestPreferenceStore by lazy {
        infrastructure.backend(
            supabase = ::SupabaseMarketplaceInterestPreferenceStore,
            inMemory = { preferences.interests }
        )
    }
    internal val reportedListingVisibilityPreferenceStore get() = preferences.reportedListingVisibility
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
    internal val marketplaceListingInsertObserver: MarketplaceListingInsertObserver by lazy {
        infrastructure.backend(
            supabase = { client ->
                PollingMarketplaceListingInsertObserver(
                    realtime = SupabaseMarketplaceListingInsertObserver(client),
                    snapshots = SupabaseMarketplaceListingIdSnapshotSource(client)
                )
            },
            inMemory = { EmptyMarketplaceListingInsertObserver }
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
    private val storedMarketplaceReportedListingProvider by lazy {
        GetMarketplaceReportedListings(reportIdentity, listingReportRepository)
    }
    private val marketplaceReportedListingProvider by lazy {
        PreferenceAwareReportedListingProvider(
            storedMarketplaceReportedListingProvider,
            reportedListingVisibilityPreferenceStore
        )
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
    internal val reviewRequestRepository by lazy {
        infrastructure.backend(
            supabase = ::SupabaseReviewRequestRepository,
            inMemory = ::InMemoryReviewRequestRepository
        )
    }
    val submitMarketplaceReview by lazy {
        SubmitMarketplaceReview(marketplaceRepository, reviewRequestRepository)
    }
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
