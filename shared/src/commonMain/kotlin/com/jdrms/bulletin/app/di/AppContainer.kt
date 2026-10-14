package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.app.integration.AuthInboxIdentity
import com.jdrms.bulletin.app.integration.AuthListingSellerProvider
import com.jdrms.bulletin.app.integration.AuthMessageSenderProvider
import com.jdrms.bulletin.app.integration.CompositeMarketplaceListingSource
import com.jdrms.bulletin.app.integration.ListingsActiveListingsCountProvider
import com.jdrms.bulletin.app.integration.ListingsMarketplaceListingSource
import com.jdrms.bulletin.app.integration.ListingsSoldListingsCountProvider
import com.jdrms.bulletin.app.integration.MarketplaceProfileBookmarkedListingsProvider
import com.jdrms.bulletin.app.integration.ProfileMarketplaceBlockedUserProvider
import com.jdrms.bulletin.app.integration.ProfileMarketplaceSellerNameProvider
import com.jdrms.bulletin.app.integration.ProfileMarketplaceUserBlocker
import com.jdrms.bulletin.app.integration.ProfileMarketplaceUserReporter
import com.jdrms.bulletin.app.theme.InMemoryThemePreferenceStore
import com.jdrms.bulletin.app.theme.ThemePreferenceStore
import com.jdrms.bulletin.app.theme.ThemeViewModel
import com.jdrms.bulletin.core.common.FlowUserMessenger
import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.network.SupabaseConfig
import com.jdrms.bulletin.domain.home.application.GetPersonalizedFeed
import com.jdrms.bulletin.domain.home.application.UpdateUserPreferences
import com.jdrms.bulletin.domain.home.infrastructure.repository.InMemoryHomeRepository
import com.jdrms.bulletin.domain.home.presentation.HomeViewModel
import com.jdrms.bulletin.domain.listings.application.CreateListing
import com.jdrms.bulletin.domain.listings.application.DeleteListing
import com.jdrms.bulletin.domain.listings.application.GetSellerListings
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.application.MarkListingSold
import com.jdrms.bulletin.domain.listings.application.RestoreListingToMarketplace
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import com.jdrms.bulletin.domain.listings.infrastructure.repository.SupabaseListingsRepository
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModel
import com.jdrms.bulletin.domain.marketplace.application.BlockMarketplaceUser
import com.jdrms.bulletin.domain.marketplace.application.BookmarkMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.application.GetMarketplaceListingBookmarks
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceRepositoryListingSource
import com.jdrms.bulletin.domain.marketplace.application.RemoveMarketplaceListingBookmark
import com.jdrms.bulletin.domain.marketplace.application.ReportMarketplaceUser
import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceSellerProfile
import com.jdrms.bulletin.domain.marketplace.domain.repository.MarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.SupabaseMarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.SupabaseMarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceBookmarkDependencies
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceUserBlockViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceUserReportViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.SellerProfileViewModel
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.application.GetConversations
import com.jdrms.bulletin.domain.messages.application.HasListingConversation
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
import com.jdrms.bulletin.domain.messages.presentation.MessagesViewModel
import com.jdrms.bulletin.domain.profile.application.*
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.repository.ProfilePhotoRepository
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository
import com.jdrms.bulletin.domain.profile.domain.repository.UserBlockRepository
import com.jdrms.bulletin.domain.profile.domain.repository.UserReportRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.AuthSessionRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfilePhotoRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryUserBlockRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryUserReportRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabaseAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabaseProfilePhotoRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabaseProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabaseUserBlockRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabaseUserReportRepository
import com.jdrms.bulletin.domain.profile.presentation.*
import io.github.jan.supabase.SupabaseClient
class AppContainer(
    val supabaseConfig: SupabaseConfig = SupabaseConfig(),
    private val isInspectionMode: Boolean = false,
    private val allowInMemoryFallback: Boolean = true,
    themePreferenceStore: ThemePreferenceStore = InMemoryThemePreferenceStore(),
    private val providedSupabaseClient: SupabaseClient? = null
) {
    private val themePreferenceStore = themePreferenceStore
    val listingChangedSignal: RefreshSignal by lazy { RefreshSignal() }
    val supabaseClient: SupabaseClient? by lazy {
        if (providedSupabaseClient != null) {
            providedSupabaseClient
        } else if (!isInspectionMode && supabaseConfig.isConfigured) {
            runCatching {
                supabaseConfig.createClient()
            }.fold(
                onSuccess = { it },
                onFailure = { error ->
                    println("Failed to initialize SupabaseClient: ${error.message}")
                    null
                }
            )
        } else {
            null
        }
    }
    val homeRepository by lazy { InMemoryHomeRepository() }
    val marketplaceRepository: MarketplaceRepository by lazy {
        val client = supabaseClient
        if (client != null) {
            SupabaseMarketplaceRepository(client)
        } else {
            if (!allowInMemoryFallback && !isInspectionMode) {
                error("Supabase client is not configured and in-memory fallback is disabled in release builds.")
            }
            InMemoryMarketplaceRepository()
        }
    }
    val listingsRepository: ListingsRepository by lazy {
        val client = supabaseClient
        if (client != null) {
            SupabaseListingsRepository(client)
        } else {
            if (!allowInMemoryFallback && !isInspectionMode) {
                error("Supabase client is not configured and in-memory fallback is disabled in release builds.")
            }
            InMemoryListingsRepository()
        }
    }
    val marketplaceListingSource: MarketplaceListingSource by lazy {
        val client = supabaseClient
        if (client != null) {
            SupabaseMarketplaceListingSource(client)
        } else {
            CompositeMarketplaceListingSource(
                ListingsMarketplaceListingSource(listingsRepository),
                MarketplaceRepositoryListingSource(marketplaceRepository)
            )
        }
    }
    val messagesRepository: MessagesRepository by lazy {
        val client = supabaseClient
        if (client != null) {
            SupabaseMessagesRepository(client)
        } else {
            if (!allowInMemoryFallback && !isInspectionMode) {
                error("Supabase client is not configured and in-memory fallback is disabled in release builds.")
            }
            InMemoryMessagesRepository()
        }
    }
    val messagesObserver: MessagesObserver by lazy {
        val client = supabaseClient
        if (client != null) {
            SupabaseMessagesObserver(client, messagesRepository)
        } else {
            messagesRepository as InMemoryMessagesRepository
        }
    }
    val inboxObserver: InboxObserver by lazy {
        supabaseClient?.let(::SupabaseInboxObserver) ?: (messagesRepository as InMemoryMessagesRepository)
    }
    val profileRepository: ProfileRepository by lazy {
        val client = supabaseClient
        if (client != null) {
            SupabaseProfileRepository(client)
        } else {
            if (!allowInMemoryFallback && !isInspectionMode) {
                error("Supabase client is not configured and in-memory fallback is disabled in release builds.")
            }
            InMemoryProfileRepository()
        }
    }
    private val userReportRepository: UserReportRepository by lazy {
        val client = supabaseClient
        if (client != null) {
            SupabaseUserReportRepository(client)
        } else {
            if (!allowInMemoryFallback && !isInspectionMode) {
                error("Supabase client is not configured and in-memory fallback is disabled in release builds.")
            }
            InMemoryUserReportRepository()
        }
    }
    private val userBlockRepository: UserBlockRepository by lazy {
        if (supabaseClient == null && !allowInMemoryFallback && !isInspectionMode) {
            error("Supabase client is not configured and in-memory fallback is disabled in release builds.")
        }
        supabaseClient?.let(::SupabaseUserBlockRepository) ?: InMemoryUserBlockRepository()
    }
    val profilePhotoRepository: ProfilePhotoRepository by lazy {
        supabaseClient?.let(::SupabaseProfilePhotoRepository) ?: InMemoryProfilePhotoRepository()
    }
    val authRepository: AuthRepository by lazy {
        val client = supabaseClient
        if (client != null) {
            SupabaseAuthRepository(client, profileRepository)
        } else {
            if (!allowInMemoryFallback && !isInspectionMode) {
                error("Supabase client is not configured and in-memory fallback is disabled in release builds.")
            }
            InMemoryAuthRepository(profileRepository)
        }
    }
    val sessionRepository by lazy { AuthSessionRepository(authRepository) }
    val userMessenger by lazy { FlowUserMessenger() }
    val getPersonalizedFeed by lazy { GetPersonalizedFeed(homeRepository) }
    val updateUserPreferences by lazy { UpdateUserPreferences(homeRepository) }
    private val marketplaceBlockedUserProvider by lazy {
        ProfileMarketplaceBlockedUserProvider(userBlockRepository)
    }
    val searchMarketplace by lazy {
        SearchMarketplace(
            repository = marketplaceRepository,
            listingSource = marketplaceListingSource,
            blockedUserProvider = marketplaceBlockedUserProvider
        )
    }
    val bookmarkMarketplaceListing by lazy { BookmarkMarketplaceListing(marketplaceRepository) }
    val getMarketplaceListingBookmarks by lazy { GetMarketplaceListingBookmarks(marketplaceRepository) }
    val removeMarketplaceListingBookmark by lazy { RemoveMarketplaceListingBookmark(marketplaceRepository) }
    private val marketplaceSellerProfileProvider by lazy { ProfileMarketplaceSellerNameProvider(profileRepository) }
    val viewMarketplaceListing by lazy {
        ViewMarketplaceListing(
            repository = marketplaceRepository,
            sellerProfileProvider = marketplaceSellerProfileProvider,
            blockedUserProvider = marketplaceBlockedUserProvider
        )
    }
    private val viewMarketplaceSellerProfile by lazy {
        ViewMarketplaceSellerProfile(marketplaceSellerProfileProvider, marketplaceListingSource)
    }
    private val submitUserReport by lazy { SubmitUserReport(authRepository, userReportRepository) }
    private val reportMarketplaceUser by lazy {
        ReportMarketplaceUser(ProfileMarketplaceUserReporter(submitUserReport))
    }
    private val blockUser by lazy { BlockUser(authRepository, userBlockRepository) }
    private val blockMarketplaceUser by lazy { BlockMarketplaceUser(ProfileMarketplaceUserBlocker(blockUser)) }
    private val unblockUser by lazy { UnblockUser(authRepository, userBlockRepository) }
    private val getBlockedUsers by lazy { GetBlockedUsers(authRepository, userBlockRepository, profileRepository) }
    val createListing by lazy { CreateListing(listingsRepository) }
    val manageListing by lazy { ManageListing(listingsRepository) }
    val markListingSold by lazy { MarkListingSold(listingsRepository) }
    val restoreListingToMarketplace by lazy { RestoreListingToMarketplace(listingsRepository) }
    val deleteListing by lazy { DeleteListing(listingsRepository) }
    val getSellerListings by lazy { GetSellerListings(listingsRepository) }
    val currentListingSellerProvider by lazy { AuthListingSellerProvider(authRepository) }
    val currentMessageSenderProvider by lazy { AuthMessageSenderProvider(restoreAuthenticatedProfile) }
    val getConversations by lazy { GetConversations(messagesRepository, currentMessageSenderProvider) }
    val hasListingConversation by lazy { HasListingConversation(messagesRepository, currentMessageSenderProvider) }
    val getConversationMessages by lazy { GetConversationMessages(messagesRepository, currentMessageSenderProvider) }
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
    val signInUser by lazy { SignInUser(authRepository) }
    val registerStudent by lazy { RegisterStudent(authRepository) }
    val restoreAuthenticatedProfile by lazy { RestoreAuthenticatedProfile(authRepository) }
    val signOutUser by lazy { SignOutUser(authRepository) }
    val resendVerificationCode by lazy { ResendVerificationCode(authRepository) }
    val verifyStudentEmail by lazy { VerifyStudentEmail(authRepository) }
    val requestPasswordReset by lazy { RequestPasswordReset(authRepository) }
    val verifyPasswordResetCode by lazy { VerifyPasswordResetCode(authRepository) }
    val updatePassword by lazy { UpdatePassword(authRepository) }
    val updateStudentProfile by lazy { UpdateStudentProfile(profileRepository) }
    val uploadProfilePhoto by lazy { UploadProfilePhoto(profilePhotoRepository, profileRepository) }
    val softDeleteProfile by lazy { SoftDeleteProfile(profileRepository, signOutUser) }
    val submitStudentReview by lazy { SubmitStudentReview(profileRepository) }
    val publishStudentReview by lazy { PublishStudentReview(submitStudentReview) }
    val profileActiveListingsProvider by lazy { ListingsActiveListingsCountProvider(listingsRepository) }
    val profileSoldListingsProvider by lazy { ListingsSoldListingsCountProvider(listingsRepository) }
    val getProfileOverview by lazy { GetProfileOverview(profileRepository, profileActiveListingsProvider) }
    val getProfileActivity by lazy { GetProfileActivity(profileActiveListingsProvider) }
    private val marketplaceProfileBookmarks by lazy {
        MarketplaceProfileBookmarkedListingsProvider(
            getMarketplaceListingBookmarks,
            removeMarketplaceListingBookmark,
            listingsRepository
        )
    }
    val profileBookmarkedListingsProvider: ProfileBookmarkedListingsProvider by lazy { marketplaceProfileBookmarks }
    val profileBookmarkedListingRemover: ProfileBookmarkedListingRemover by lazy { marketplaceProfileBookmarks }
    fun createHomeViewModel() = HomeViewModel(
        getPersonalizedFeed = getPersonalizedFeed,
        updateUserPreferences = updateUserPreferences
    )
    fun createMarketplaceViewModel() = MarketplaceViewModel(
        searchMarketplace = searchMarketplace,
        bookmarks = MarketplaceBookmarkDependencies(
            add = bookmarkMarketplaceListing,
            remove = removeMarketplaceListingBookmark,
            get = getMarketplaceListingBookmarks
        ),
        viewMarketplaceListing = viewMarketplaceListing,
        listingChangedSignal = listingChangedSignal,
        currentUserIdProvider = {
            when (val result = authRepository.getCurrentUserId()) {
                is com.jdrms.bulletin.core.common.Result.Success -> result.data?.value
                is com.jdrms.bulletin.core.common.Result.Error -> null
            }
        }
    )
    fun createMarketplaceUserReportViewModel() = MarketplaceUserReportViewModel(reportMarketplaceUser)
    val marketplaceUserBlockViewModelFactory: () -> MarketplaceUserBlockViewModel = {
        MarketplaceUserBlockViewModel(blockMarketplaceUser)
    }
    val blockedUsersViewModelFactory: () -> BlockedUsersViewModel = {
        BlockedUsersViewModel(getBlockedUsers, unblockUser, listingChangedSignal)
    }
    fun createSellerProfileViewModel() = SellerProfileViewModel(viewMarketplaceSellerProfile)
    fun createListingsViewModel() = ListingsViewModel(
        createListing = createListing,
        manageListing = manageListing,
        markListingSold = markListingSold,
        restoreListingToMarketplace = restoreListingToMarketplace,
        deleteListing = deleteListing,
        getSellerListings = getSellerListings,
        currentSellerProvider = currentListingSellerProvider,
        listingChangedSignal = listingChangedSignal
    )
    fun createMessagesViewModel() = MessagesViewModel(
        inbox = inboxStateOwner,
        getConversationMessages = getConversationMessages,
        observeConversationMessages = observeConversationMessages,
        sendMessage = sendMessage,
        reportMessage = reportMessage
    )
    fun createSignInViewModel() = SignInViewModel(signInUser, sessionRepository)
    fun createRegistrationViewModel() = RegistrationViewModel(
        registerStudent,
        verifyStudentEmail,
        resendVerificationCode,
        sessionRepository
    )
    fun createProfileViewModel() = ProfileViewModel(
        sessionRepository = sessionRepository,
        getProfileOverview = getProfileOverview,
        getProfileActivity = getProfileActivity,
        listingChangedSignal = listingChangedSignal,
        soldListingsProvider = profileSoldListingsProvider,
        bookmarks = ProfileBookmarksDependencies(
            provider = profileBookmarkedListingsProvider,
            remover = profileBookmarkedListingRemover
        )
    )
    fun createEditProfileViewModel() = EditProfileViewModel(
        sessionRepository,
        updateStudentProfile,
        uploadProfilePhoto,
        userMessenger
    )
    fun createReviewViewModel() = ReviewViewModel(sessionRepository, publishStudentReview)
    fun createAccountViewModel() = AccountViewModel(sessionRepository, signOutUser, softDeleteProfile, userMessenger)
    fun createPasswordRecoveryViewModel() = PasswordRecoveryViewModel(
        requestPasswordResetUseCase = requestPasswordReset,
        verifyPasswordResetCodeUseCase = verifyPasswordResetCode,
        updatePasswordUseCase = updatePassword,
        signOutUser = signOutUser
    )
    fun createThemeViewModel() = ThemeViewModel(themePreferenceStore)
}
