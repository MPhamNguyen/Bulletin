package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.app.integration.AuthListingSellerProvider
import com.jdrms.bulletin.app.integration.AuthMessageSenderProvider
import com.jdrms.bulletin.app.integration.CompositeMarketplaceListingSource
import com.jdrms.bulletin.app.integration.ListingsActiveListingsCountProvider
import com.jdrms.bulletin.app.integration.ListingsMarketplaceListingSource
import com.jdrms.bulletin.app.integration.ProfileMarketplaceSellerNameProvider
import com.jdrms.bulletin.app.theme.InMemoryThemePreferenceStore
import com.jdrms.bulletin.app.theme.ThemePreferenceStore
import com.jdrms.bulletin.app.theme.ThemeViewModel
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
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import com.jdrms.bulletin.domain.listings.infrastructure.repository.SupabaseListingsRepository
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModel
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceRepositoryListingSource
import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.application.ToggleSaveMarketplaceItem
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.domain.repository.MarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.SupabaseMarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.SupabaseMarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceViewModel
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.application.GetConversations
import com.jdrms.bulletin.domain.messages.application.ReportMessage
import com.jdrms.bulletin.domain.messages.application.SendMessage
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.repository.InMemoryMessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesRepository
import com.jdrms.bulletin.domain.messages.presentation.MessagesViewModel
import com.jdrms.bulletin.domain.profile.application.AuthenticateUser
import com.jdrms.bulletin.domain.profile.application.GetAuthenticatedUserId
import com.jdrms.bulletin.domain.profile.application.ManageProfile
import com.jdrms.bulletin.domain.profile.application.ResendVerificationCode
import com.jdrms.bulletin.domain.profile.application.RestoreAuthenticatedProfile
import com.jdrms.bulletin.domain.profile.application.SignOutUser
import com.jdrms.bulletin.domain.profile.application.SubmitStudentReview
import com.jdrms.bulletin.domain.profile.application.UpdateStudentProfile
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabaseAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabaseProfileRepository
import com.jdrms.bulletin.domain.profile.presentation.ProfileViewModel
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

    // Repositories
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

    // Use Cases - Home
    val getPersonalizedFeed by lazy { GetPersonalizedFeed(homeRepository) }
    val updateUserPreferences by lazy { UpdateUserPreferences(homeRepository) }

    // Use Cases - Marketplace
    val searchMarketplace by lazy { SearchMarketplace(marketplaceRepository, marketplaceListingSource) }
    val toggleSaveMarketplaceItem by lazy { ToggleSaveMarketplaceItem(marketplaceRepository) }
    val viewMarketplaceListing by lazy {
        ViewMarketplaceListing(
            repository = marketplaceRepository,
            sellerProfileProvider = ProfileMarketplaceSellerNameProvider(profileRepository)
        )
    }

    // Use Cases - Listings
    val createListing by lazy { CreateListing(listingsRepository) }
    val manageListing by lazy { ManageListing(listingsRepository) }
    val deleteListing by lazy { DeleteListing(listingsRepository) }
    val getSellerListings by lazy { GetSellerListings(listingsRepository) }
    val currentListingSellerProvider by lazy { AuthListingSellerProvider(authRepository) }

    // Use Cases - Messages
    val currentMessageSenderProvider by lazy { AuthMessageSenderProvider(restoreAuthenticatedProfile) }
    val getConversations by lazy { GetConversations(messagesRepository, currentMessageSenderProvider) }
    val getConversationMessages by lazy { GetConversationMessages(messagesRepository, currentMessageSenderProvider) }
    val sendMessage by lazy { SendMessage(messagesRepository, currentMessageSenderProvider) }
    val reportMessage by lazy { ReportMessage(messagesRepository, currentMessageSenderProvider) }

    // Use Cases - Profile
    val authenticateUser by lazy { AuthenticateUser(authRepository) }
    val getAuthenticatedUserId by lazy { GetAuthenticatedUserId(authRepository) }
    val restoreAuthenticatedProfile by lazy { RestoreAuthenticatedProfile(authRepository) }
    val signOutUser by lazy { SignOutUser(authRepository) }
    val resendVerificationCode by lazy { ResendVerificationCode(authRepository) }
    val verifyStudentEmail by lazy { VerifyStudentEmail(authRepository) }
    val manageProfile by lazy { ManageProfile(profileRepository) }
    val updateStudentProfile by lazy { UpdateStudentProfile(profileRepository) }
    val submitStudentReview by lazy { SubmitStudentReview(profileRepository) }
    val profileActiveListingsProvider by lazy { ListingsActiveListingsCountProvider(listingsRepository) }

    // ViewModels
    fun createHomeViewModel() = HomeViewModel(
        getPersonalizedFeed = getPersonalizedFeed,
        updateUserPreferences = updateUserPreferences
    )

    fun createMarketplaceViewModel() = MarketplaceViewModel(
        searchMarketplace = searchMarketplace,
        toggleSaveItem = toggleSaveMarketplaceItem,
        viewMarketplaceListing = viewMarketplaceListing,
        listingChangedSignal = listingChangedSignal
    )

    fun createListingsViewModel() = ListingsViewModel(
        createListing = createListing,
        manageListing = manageListing,
        deleteListing = deleteListing,
        getSellerListings = getSellerListings,
        currentSellerProvider = currentListingSellerProvider,
        listingChangedSignal = listingChangedSignal
    )

    fun createMessagesViewModel() = MessagesViewModel(
        getConversations = getConversations,
        getConversationMessages = getConversationMessages,
        sendMessage = sendMessage,
        reportMessage = reportMessage
    )

    fun createProfileViewModel() = ProfileViewModel(
        authenticateUser = authenticateUser,
        restoreAuthenticatedProfile = restoreAuthenticatedProfile,
        signOutUser = signOutUser,
        verifyStudentEmail = verifyStudentEmail,
        resendVerificationCode = resendVerificationCode,
        manageProfile = manageProfile,
        updateStudentProfile = updateStudentProfile,
        submitStudentReview = submitStudentReview,
        activeListingsProvider = profileActiveListingsProvider,
        listingChangedSignal = listingChangedSignal
    )

    fun createThemeViewModel() = ThemeViewModel(themePreferenceStore)
}
