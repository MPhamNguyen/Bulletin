package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.core.network.SupabaseConfig
import com.jdrms.bulletin.domain.home.application.GetPersonalizedFeed
import com.jdrms.bulletin.domain.home.application.UpdateUserPreferences
import com.jdrms.bulletin.domain.home.infrastructure.repository.InMemoryHomeRepository
import com.jdrms.bulletin.domain.home.presentation.HomeViewModel
import com.jdrms.bulletin.domain.listings.application.CreateListing
import com.jdrms.bulletin.domain.listings.application.GetSellerListings
import com.jdrms.bulletin.domain.listings.application.ManageListing
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModel
import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.application.ToggleSaveMarketplaceItem
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceViewModel
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.application.GetConversations
import com.jdrms.bulletin.domain.messages.application.ReportMessage
import com.jdrms.bulletin.domain.messages.application.SendMessage
import com.jdrms.bulletin.domain.messages.infrastructure.repository.InMemoryMessagesRepository
import com.jdrms.bulletin.domain.messages.presentation.MessagesViewModel
import com.jdrms.bulletin.domain.profile.application.AuthenticateUser
import com.jdrms.bulletin.domain.profile.application.ManageProfile
import com.jdrms.bulletin.domain.profile.application.SubmitStudentReview
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
) {
    val supabaseClient: SupabaseClient? by lazy {
        if (!isInspectionMode && supabaseConfig.isConfigured) {
            runCatching { supabaseConfig.createClient() }.getOrNull()
        } else {
            null
        }
    }

    // Repositories
    val homeRepository by lazy { InMemoryHomeRepository() }
    val marketplaceRepository by lazy { InMemoryMarketplaceRepository() }
    val listingsRepository by lazy { InMemoryListingsRepository() }
    val messagesRepository by lazy { InMemoryMessagesRepository() }
    val profileRepository: ProfileRepository by lazy {
        val client = supabaseClient
        if (client != null && !isInspectionMode) {
            SupabaseProfileRepository(client)
        } else {
            InMemoryProfileRepository()
        }
    }
    val authRepository: AuthRepository by lazy {
        val client = supabaseClient
        if (client != null && !isInspectionMode) {
            SupabaseAuthRepository(client, profileRepository)
        } else {
            InMemoryAuthRepository(profileRepository)
        }
    }

    // Use Cases - Home
    val getPersonalizedFeed by lazy { GetPersonalizedFeed(homeRepository) }
    val updateUserPreferences by lazy { UpdateUserPreferences(homeRepository) }

    // Use Cases - Marketplace
    val searchMarketplace by lazy { SearchMarketplace(marketplaceRepository) }
    val toggleSaveMarketplaceItem by lazy { ToggleSaveMarketplaceItem(marketplaceRepository) }

    // Use Cases - Listings
    val createListing by lazy { CreateListing(listingsRepository) }
    val manageListing by lazy { ManageListing(listingsRepository) }
    val getSellerListings by lazy { GetSellerListings(listingsRepository) }

    // Use Cases - Messages
    val getConversations by lazy { GetConversations(messagesRepository) }
    val getConversationMessages by lazy { GetConversationMessages(messagesRepository) }
    val sendMessage by lazy { SendMessage(messagesRepository) }
    val reportMessage by lazy { ReportMessage(messagesRepository) }

    // Use Cases - Profile
    val authenticateUser by lazy { AuthenticateUser(authRepository) }
    val verifyStudentEmail by lazy { VerifyStudentEmail(authRepository) }
    val manageProfile by lazy { ManageProfile(profileRepository) }
    val submitStudentReview by lazy { SubmitStudentReview(profileRepository) }

    // ViewModels
    fun createHomeViewModel() = HomeViewModel(
        getPersonalizedFeed = getPersonalizedFeed,
        updateUserPreferences = updateUserPreferences
    )

    fun createMarketplaceViewModel() = MarketplaceViewModel(
        searchMarketplace = searchMarketplace,
        toggleSaveItem = toggleSaveMarketplaceItem
    )

    fun createListingsViewModel() = ListingsViewModel(
        createListing = createListing,
        manageListing = manageListing,
        getSellerListings = getSellerListings
    )

    fun createMessagesViewModel() = MessagesViewModel(
        getConversations = getConversations,
        getConversationMessages = getConversationMessages,
        sendMessage = sendMessage,
        reportMessage = reportMessage
    )

    fun createProfileViewModel() = ProfileViewModel(
        authenticateUser = authenticateUser,
        verifyStudentEmail = verifyStudentEmail,
        manageProfile = manageProfile,
        submitStudentReview = submitStudentReview
    )
}
