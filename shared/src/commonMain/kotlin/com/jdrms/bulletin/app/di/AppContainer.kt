package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.core.network.SupabaseConfig
import com.jdrms.bulletin.domain.identity.application.AuthenticateUser
import com.jdrms.bulletin.domain.identity.application.ManageProfile
import com.jdrms.bulletin.domain.identity.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.identity.infrastructure.repository.SupabaseAuthRepository
import com.jdrms.bulletin.domain.identity.infrastructure.repository.SupabaseProfileRepository
import com.jdrms.bulletin.domain.identity.presentation.IdentityViewModel
import com.jdrms.bulletin.domain.marketplace.application.CreateListing
import com.jdrms.bulletin.domain.marketplace.application.ManageListing
import com.jdrms.bulletin.domain.marketplace.application.SearchListings
import com.jdrms.bulletin.domain.marketplace.application.ToggleFavorite
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.SupabaseListingRepository
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceViewModel
import com.jdrms.bulletin.domain.messaging.application.GetConversations
import com.jdrms.bulletin.domain.messaging.application.ReportMessage
import com.jdrms.bulletin.domain.messaging.application.SendMessage
import com.jdrms.bulletin.domain.messaging.infrastructure.repository.SupabaseMessagingRepository
import com.jdrms.bulletin.domain.messaging.presentation.MessagingViewModel
import com.jdrms.bulletin.domain.recommendations.application.GetPersonalizedFeed
import com.jdrms.bulletin.domain.recommendations.application.UpdateUserPreferences
import com.jdrms.bulletin.domain.recommendations.infrastructure.repository.SupabaseRecommendationRepository
import com.jdrms.bulletin.domain.recommendations.presentation.RecommendationsViewModel
import com.jdrms.bulletin.domain.reputation.application.GetStudentReputation
import com.jdrms.bulletin.domain.reputation.application.SubmitReview
import com.jdrms.bulletin.domain.reputation.infrastructure.repository.SupabaseReputationRepository
import com.jdrms.bulletin.domain.reputation.presentation.ReputationViewModel

class AppContainer(
    val supabaseConfig: SupabaseConfig = SupabaseConfig()
) {
    // Repositories
    val authRepository by lazy { SupabaseAuthRepository(supabaseConfig) }
    val profileRepository by lazy { SupabaseProfileRepository(supabaseConfig) }
    val listingRepository by lazy { SupabaseListingRepository(supabaseConfig) }
    val messagingRepository by lazy { SupabaseMessagingRepository(supabaseConfig) }
    val reputationRepository by lazy { SupabaseReputationRepository(supabaseConfig) }
    val recommendationRepository by lazy { SupabaseRecommendationRepository(listingRepository, supabaseConfig = supabaseConfig) }

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
