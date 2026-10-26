package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.app.application.GetVisibleMarketplaceListings
import com.jdrms.bulletin.app.integration.AuthInboxIdentity
import com.jdrms.bulletin.app.integration.AuthListingSellerProvider
import com.jdrms.bulletin.app.integration.AuthMarketplaceReportIdentity
import com.jdrms.bulletin.app.integration.AuthMessageSenderProvider
import com.jdrms.bulletin.app.integration.CompositeMarketplaceListingSource
import com.jdrms.bulletin.app.integration.ListingsActiveListingsCountProvider
import com.jdrms.bulletin.app.integration.ListingsMarketplaceListingSource
import com.jdrms.bulletin.app.integration.ListingsMarketplaceSellerSalesProvider
import com.jdrms.bulletin.app.integration.ListingsSoldListingsCountProvider
import com.jdrms.bulletin.app.integration.MarketplaceConversationListingPreviewProvider
import com.jdrms.bulletin.app.integration.MarketplaceHomeRepository
import com.jdrms.bulletin.app.integration.MarketplaceHomeSaveAdapter
import com.jdrms.bulletin.app.integration.MarketplaceProfileBookmarkedListingsProvider
import com.jdrms.bulletin.app.integration.MessagesHomeContactSellerAdapter
import com.jdrms.bulletin.app.integration.ProfileMarketplaceSellerNameProvider
import com.jdrms.bulletin.app.theme.InMemoryThemePreferenceStore
import com.jdrms.bulletin.app.theme.ThemePreferenceStore
import com.jdrms.bulletin.core.common.FlowUserMessenger
import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.network.SupabaseConfig
import com.jdrms.bulletin.domain.home.application.GetPersonalizedFeed
import com.jdrms.bulletin.domain.home.application.RecordSwipeDecision
import com.jdrms.bulletin.domain.home.application.SendQuickMessage
import com.jdrms.bulletin.domain.home.application.UpdateUserPreferences
import com.jdrms.bulletin.domain.home.infrastructure.repository.InMemorySwipeDecisionRepository
import com.jdrms.bulletin.domain.listings.application.CreateListing
import com.jdrms.bulletin.domain.listings.application.DeleteListing
import com.jdrms.bulletin.domain.listings.application.GetSellerListings
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.application.ManageListingImages
import com.jdrms.bulletin.domain.listings.application.MarkListingSold
import com.jdrms.bulletin.domain.listings.application.RestoreListingToMarketplace
import com.jdrms.bulletin.domain.listings.domain.repository.ListingImagesRepository
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingImagesRepository
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import com.jdrms.bulletin.domain.listings.infrastructure.repository.SupabaseListingImagesRepository
import com.jdrms.bulletin.domain.listings.infrastructure.repository.SupabaseListingsRepository
import com.jdrms.bulletin.domain.marketplace.application.BlockMarketplaceUser
import com.jdrms.bulletin.domain.marketplace.application.BookmarkMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.application.GetMarketplaceListingBookmarks
import com.jdrms.bulletin.domain.marketplace.application.GetMarketplaceReportedListings
import com.jdrms.bulletin.domain.marketplace.application.InMemoryMarketplaceLayoutPreferenceStore
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
import com.jdrms.bulletin.domain.messages.application.ConsoleMessagesInstrumentation
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.application.GetConversations
import com.jdrms.bulletin.domain.messages.application.HasListingConversation
import com.jdrms.bulletin.domain.messages.application.HideConversation
import com.jdrms.bulletin.domain.messages.application.MarkConversationRead
import com.jdrms.bulletin.domain.messages.application.MessageSeller
import com.jdrms.bulletin.domain.messages.application.ObserveConversationMessages
import com.jdrms.bulletin.domain.messages.application.ObserveInboxInvalidations
import com.jdrms.bulletin.domain.messages.application.ReportMessage
import com.jdrms.bulletin.domain.messages.application.SendMessage
import com.jdrms.bulletin.domain.messages.domain.repository.InboxObserver
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesObserver
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.repository.InMemoryMessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseInboxObserver
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesObserver
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesRepository
import com.jdrms.bulletin.domain.messages.presentation.InboxStateOwner
import com.jdrms.bulletin.domain.profile.application.GetProfileActivity
import com.jdrms.bulletin.domain.profile.application.GetProfileOverview
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListingRemover
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListingsProvider
import io.github.jan.supabase.SupabaseClient

class AppContainer(
    val supabaseConfig: SupabaseConfig = SupabaseConfig(),
    isInspectionMode: Boolean = false,
    allowInMemoryFallback: Boolean = false,
    themePreferenceStore: ThemePreferenceStore = InMemoryThemePreferenceStore(),
    marketplaceLayoutPreferenceStore: MarketplaceLayoutPreferenceStore =
        InMemoryMarketplaceLayoutPreferenceStore(),
    providedSupabaseClient: SupabaseClient? = null
) {
    internal val themePreferenceStore = themePreferenceStore
    internal val marketplaceLayoutPreferenceStore = marketplaceLayoutPreferenceStore
    val listingChangedSignal: RefreshSignal by lazy { RefreshSignal() }
    private val marketplaceReadCache by lazy { MarketplaceReadCache() }
    private val infrastructure = InfrastructureModule(
        supabaseConfig,
        isInspectionMode,
        allowInMemoryFallback,
        providedSupabaseClient
    )
    private val profile = ProfileModule(infrastructure)
    val supabaseClient: SupabaseClient? get() = infrastructure.client

    val getVisibleMarketplaceListings by lazy {
        GetVisibleMarketplaceListings(
            listingSource = marketplaceListingSource,
            blockedUserProvider = marketplaceBlockedUserProvider,
            reportedListingProvider = marketplaceReportedListingProvider
        )
    }
    val homeRepository by lazy { MarketplaceHomeRepository(getVisibleMarketplaceListings) }
    val marketplaceRepository: MarketplaceRepository by lazy {
        infrastructure.backend(
            supabase = { SupabaseMarketplaceRepository(it, readCache = marketplaceReadCache) },
            inMemory = { InMemoryMarketplaceRepository() }
        )
    }
    val listingsRepository: ListingsRepository by lazy {
        infrastructure.backend(
            supabase = { client -> SupabaseListingsRepository(client) { marketplaceReadCache.invalidateAll() } },
            inMemory = { InMemoryListingsRepository() }
        )
    }
    val listingImagesRepository: ListingImagesRepository by lazy {
        infrastructure.backend(
            supabase = ::SupabaseListingImagesRepository,
            inMemory = ::InMemoryListingImagesRepository
        )
    }
    val marketplaceListingSource: MarketplaceListingSource by lazy {
        infrastructure.backend(
            supabase = { SupabaseMarketplaceListingSource(it, marketplaceReadCache) },
            inMemory = {
                CompositeMarketplaceListingSource(
                    ListingsMarketplaceListingSource(listingsRepository),
                    MarketplaceRepositoryListingSource(marketplaceRepository)
                )
            }
        )
    }
    private val inMemoryMessages by lazy { InMemoryMessagesRepository() }
    val messagesRepository: MessagesRepository by lazy {
        infrastructure.backend(
            supabase = { SupabaseMessagesRepository(it, instrumentation = ConsoleMessagesInstrumentation) },
            inMemory = { inMemoryMessages }
        )
    }
    val messagesObserver: MessagesObserver by lazy {
        infrastructure.backend(
            supabase = { SupabaseMessagesObserver(it, messagesRepository) },
            inMemory = { inMemoryMessages }
        )
    }
    val inboxObserver: InboxObserver by lazy {
        infrastructure.backend(
            supabase = ::SupabaseInboxObserver,
            inMemory = { inMemoryMessages }
        )
    }
    val profileRepository get() = profile.profileRepository
    val profilePhotoRepository get() = profile.profilePhotoRepository
    val authRepository get() = profile.authRepository
    val sessionRepository get() = profile.sessionRepository
    val userMessenger by lazy { FlowUserMessenger() }
    val getPersonalizedFeed by lazy { GetPersonalizedFeed(homeRepository) }
    val updateUserPreferences by lazy { UpdateUserPreferences(homeRepository) }
    val swipeDecisionRepository by lazy { InMemorySwipeDecisionRepository() }
    val homeSaveListing by lazy { MarketplaceHomeSaveAdapter(bookmarkMarketplaceListing) }
    val homeContactSeller by lazy {
        MessagesHomeContactSellerAdapter(viewMarketplaceListing, getVisibleMarketplaceListings, messageSeller)
    }
    val recordSwipeDecision by lazy { RecordSwipeDecision(swipeDecisionRepository) }
    val sendQuickMessage by lazy { SendQuickMessage(homeContactSeller) }
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
    private val marketplaceSellerProfileProvider by lazy { ProfileMarketplaceSellerNameProvider(profileRepository) }
    private val listingReportRepository: ListingReportRepository by lazy {
        infrastructure.backend(
            supabase = ::SupabaseListingReportRepository,
            inMemory = ::InMemoryListingReportRepository
        )
    }
    private val marketplaceReportedListingProvider by lazy {
        GetMarketplaceReportedListings(AuthMarketplaceReportIdentity(authRepository), listingReportRepository)
    }
    val reportMarketplaceListing by lazy {
        ReportMarketplaceListing(AuthMarketplaceReportIdentity(authRepository), listingReportRepository)
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
            ListingsMarketplaceSellerSalesProvider(listingsRepository)
        )
    }

    internal val reportMarketplaceUser by lazy {
        ReportMarketplaceUser(profile.marketplaceUserReporter)
    }
    internal val blockMarketplaceUser by lazy { BlockMarketplaceUser(profile.marketplaceUserBlocker) }
    internal val unblockUser get() = profile.unblockUser
    internal val getBlockedUsers get() = profile.getBlockedUsers
    val createListing by lazy { CreateListing(listingsRepository) }
    val manageListing by lazy { ManageListing(listingsRepository) }
    val manageListingImages by lazy { ManageListingImages(listingImagesRepository) }
    val markListingSold by lazy { MarkListingSold(listingsRepository) }
    val restoreListingToMarketplace by lazy { RestoreListingToMarketplace(listingsRepository) }
    val deleteListing by lazy { DeleteListing(listingsRepository) }
    val getSellerListings by lazy { GetSellerListings(listingsRepository) }
    val currentListingSellerProvider by lazy { AuthListingSellerProvider(authRepository) }
    val currentMessageSenderProvider by lazy { AuthMessageSenderProvider(restoreAuthenticatedProfile) }
    val getConversations by lazy { GetConversations(messagesRepository, currentMessageSenderProvider) }
    val conversationListingPreviewProvider by lazy {
        MarketplaceConversationListingPreviewProvider(listingsRepository, marketplaceRepository)
    }
    internal val recentListingBuyersProvider by lazy { createRecentListingBuyersProvider(getConversations) }
    val hideConversation by lazy { HideConversation(messagesRepository, currentMessageSenderProvider) }
    val hasListingConversation by lazy { HasListingConversation(messagesRepository, currentMessageSenderProvider) }
    val getConversationMessages by lazy { GetConversationMessages(messagesRepository, currentMessageSenderProvider) }
    val markConversationRead by lazy { MarkConversationRead(messagesRepository, currentMessageSenderProvider) }
    val observeConversationMessages by lazy {
        val observer = messagesObserver
        val sender = currentMessageSenderProvider
        ObserveConversationMessages(observer, sender)
    }
    val observeInboxInvalidations by lazy { ObserveInboxInvalidations(inboxObserver) }
    val inboxStateOwner by lazy {
        InboxStateOwner(getConversations, observeInboxInvalidations, AuthInboxIdentity(sessionRepository))
    }
    val sendMessage by lazy { SendMessage(messagesRepository, currentMessageSenderProvider) }
    val messageSeller by lazy { MessageSeller(messagesRepository, currentMessageSenderProvider) }
    val reportMessage by lazy { ReportMessage(messagesRepository, currentMessageSenderProvider) }
    val signInUser get() = profile.signInUser
    val registerStudent get() = profile.registerStudent
    val restoreAuthenticatedProfile get() = profile.restoreAuthenticatedProfile
    val signOutUser get() = profile.signOutUser
    val resendVerificationCode get() = profile.resendVerificationCode
    val verifyStudentEmail get() = profile.verifyStudentEmail
    val requestPasswordReset get() = profile.requestPasswordReset
    val verifyPasswordResetCode get() = profile.verifyPasswordResetCode
    val updatePassword get() = profile.updatePassword
    val updateStudentProfile get() = profile.updateStudentProfile
    val uploadProfilePhoto get() = profile.uploadProfilePhoto
    val softDeleteProfile get() = profile.softDeleteProfile
    val submitStudentReview get() = profile.submitStudentReview
    val publishStudentReview get() = profile.publishStudentReview
    val profileActiveListingsProvider by lazy { ListingsActiveListingsCountProvider(listingsRepository) }
    val profileSoldListingsProvider by lazy { ListingsSoldListingsCountProvider(listingsRepository) }
    val getProfileOverview by lazy { GetProfileOverview(profileRepository, profileActiveListingsProvider) }
    val getProfileActivity by lazy { GetProfileActivity(profileActiveListingsProvider) }
    internal val marketplaceProfileBookmarks by lazy {
        MarketplaceProfileBookmarkedListingsProvider(
            getMarketplaceListingBookmarks,
            removeMarketplaceListingBookmark,
            listingsRepository
        )
    }
    val profileBookmarkedListingsProvider: ProfileBookmarkedListingsProvider by lazy { marketplaceProfileBookmarks }
    val profileBookmarkedListingRemover: ProfileBookmarkedListingRemover by lazy { marketplaceProfileBookmarks }
}
