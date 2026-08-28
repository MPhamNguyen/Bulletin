package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.core.network.SupabaseConfig
import com.jdrms.bulletin.domain.create_listings.application.GetStudentReputation
import com.jdrms.bulletin.domain.create_listings.application.SubmitReview
import com.jdrms.bulletin.domain.create_listings.infrastructure.repository.InMemoryReputationRepository
import com.jdrms.bulletin.domain.create_listings.presentation.ReputationViewModel
import com.jdrms.bulletin.domain.home.application.GetPersonalizedFeed
import com.jdrms.bulletin.domain.home.application.UpdateUserPreferences
import com.jdrms.bulletin.domain.home.infrastructure.repository.InMemoryRecommendationRepository
import com.jdrms.bulletin.domain.home.presentation.RecommendationsViewModel
import com.jdrms.bulletin.domain.inbox.application.GetConversations
import com.jdrms.bulletin.domain.inbox.application.ReportMessage
import com.jdrms.bulletin.domain.inbox.application.SendMessage
import com.jdrms.bulletin.domain.inbox.infrastructure.repository.InMemoryMessagingRepository
import com.jdrms.bulletin.domain.inbox.presentation.MessagingViewModel
import com.jdrms.bulletin.domain.marketplace.application.CreateListing
import com.jdrms.bulletin.domain.marketplace.application.ManageListing
import com.jdrms.bulletin.domain.marketplace.application.SearchListings
import com.jdrms.bulletin.domain.marketplace.application.ToggleFavorite
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryListingRepository
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceViewModel
import com.jdrms.bulletin.domain.profile.application.AuthenticateUser
import com.jdrms.bulletin.domain.profile.application.ManageProfile
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import com.jdrms.bulletin.domain.profile.presentation.IdentityViewModel

class AppContainer(
    // Injection seam for the future Supabase-backed repositories. The in-memory
    // implementations below ignore it; swap them for Supabase*Repository impls when the
    // KMP Supabase SDK is wired up.
    val supabaseConfig: SupabaseConfig = SupabaseConfig()
) {
    // Repositories (in-memory development implementations)
    val authRepository by lazy { InMemoryAuthRepository() }
    val profileRepository by lazy { InMemoryProfileRepository() }
    val listingRepository by lazy { InMemoryListingRepository() }
    val messagingRepository by lazy { InMemoryMessagingRepository() }
    val reputationRepository by lazy { InMemoryReputationRepository() }
    val recommendationRepository by lazy { InMemoryRecommendationRepository(listingRepository) }

    // Use Cases - Identity
    val authenticateUser by lazy { AuthenticateUser(authRepository) }
    val verifyStudentEmail by lazy { VerifyStudentEmail(authRepository) }
    val manageProfile by lazy { ManageProfile(profileRepository) }

    // Use Cases - Marketplace
    val searchListings by lazy { SearchListings(listingRepository) }
    val createListing by lazy { CreateListing(listingRepository) }
    val manageListing by lazy { ManageListing(listingRepository) }
    val toggleFavorite by lazy { ToggleFavorite(listingRepository) }

    // Use Cases - Messaging
    val getConversations by lazy { GetConversations(messagingRepository) }
    val sendMessage by lazy { SendMessage(messagingRepository) }
    val reportMessage by lazy { ReportMessage(messagingRepository) }

    // Use Cases - Reputation
    val getStudentReputation by lazy { GetStudentReputation(reputationRepository) }
    val submitReview by lazy { SubmitReview(reputationRepository) }

    // Use Cases - Recommendations
    val getPersonalizedFeed by lazy { GetPersonalizedFeed(recommendationRepository) }
    val updateUserPreferences by lazy { UpdateUserPreferences(recommendationRepository) }

    // ViewModels
    fun createIdentityViewModel() = IdentityViewModel(
        authenticateUser = authenticateUser,
        verifyStudentEmail = verifyStudentEmail,
        manageProfile = manageProfile
    )

    fun createMarketplaceViewModel() = MarketplaceViewModel(
        searchListings = searchListings,
        createListing = createListing,
        manageListing = manageListing,
        toggleFavorite = toggleFavorite
    )

    fun createMessagingViewModel() = MessagingViewModel(
        getConversations = getConversations,
        sendMessage = sendMessage,
        reportMessage = reportMessage
    )

    fun createReputationViewModel() = ReputationViewModel(
        getStudentReputation = getStudentReputation,
        submitReview = submitReview
    )

    fun createRecommendationsViewModel() = RecommendationsViewModel(
        getPersonalizedFeed = getPersonalizedFeed,
        updateUserPreferences = updateUserPreferences
    )
}
