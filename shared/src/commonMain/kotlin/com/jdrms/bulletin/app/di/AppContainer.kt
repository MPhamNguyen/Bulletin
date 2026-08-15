package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.core.network.SupabaseConfig
import com.jdrms.bulletin.domain.identity.application.AuthenticateUser
import com.jdrms.bulletin.domain.identity.application.ManageProfile
import com.jdrms.bulletin.domain.identity.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.identity.infrastructure.repository.InMemoryAuthRepository
import com.jdrms.bulletin.domain.identity.infrastructure.repository.InMemoryProfileRepository
import com.jdrms.bulletin.domain.identity.presentation.IdentityViewModel
import com.jdrms.bulletin.domain.marketplace.application.CreateListing
import com.jdrms.bulletin.domain.marketplace.application.ManageListing
import com.jdrms.bulletin.domain.marketplace.application.SearchListings
import com.jdrms.bulletin.domain.marketplace.application.ToggleFavorite
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryListingRepository
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceViewModel
import com.jdrms.bulletin.domain.messaging.application.GetConversations
import com.jdrms.bulletin.domain.messaging.application.ReportMessage
import com.jdrms.bulletin.domain.messaging.application.SendMessage
import com.jdrms.bulletin.domain.messaging.infrastructure.repository.InMemoryMessagingRepository
import com.jdrms.bulletin.domain.messaging.presentation.MessagingViewModel
import com.jdrms.bulletin.domain.recommendations.application.GetPersonalizedFeed
import com.jdrms.bulletin.domain.recommendations.application.UpdateUserPreferences
import com.jdrms.bulletin.domain.recommendations.infrastructure.repository.InMemoryRecommendationRepository
import com.jdrms.bulletin.domain.recommendations.presentation.RecommendationsViewModel
import com.jdrms.bulletin.domain.reputation.application.GetStudentReputation
import com.jdrms.bulletin.domain.reputation.application.SubmitReview
import com.jdrms.bulletin.domain.reputation.infrastructure.repository.InMemoryReputationRepository
import com.jdrms.bulletin.domain.reputation.presentation.ReputationViewModel

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
