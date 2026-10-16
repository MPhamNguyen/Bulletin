package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.domain.home.presentation.HomeViewModel
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModel
import com.jdrms.bulletin.domain.listings.presentation.ListingsViewModelSessionDependencies
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceBookmarkDependencies
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceUserBlockViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceUserReportViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceViewModel
import com.jdrms.bulletin.domain.marketplace.presentation.SellerProfileViewModel
import com.jdrms.bulletin.domain.messages.presentation.MessagesViewModel
import com.jdrms.bulletin.domain.profile.presentation.AccountViewModel
import com.jdrms.bulletin.domain.profile.presentation.BlockedUsersViewModel
import com.jdrms.bulletin.domain.profile.presentation.EditProfileViewModel
import com.jdrms.bulletin.domain.profile.presentation.PasswordRecoveryViewModel
import com.jdrms.bulletin.domain.profile.presentation.ProfileBookmarksDependencies
import com.jdrms.bulletin.domain.profile.presentation.ProfileViewModel
import com.jdrms.bulletin.domain.profile.presentation.RegistrationViewModel
import com.jdrms.bulletin.domain.profile.presentation.ReviewViewModel
import com.jdrms.bulletin.domain.profile.presentation.SignInViewModel

fun AppContainer.createHomeViewModel() = HomeViewModel(
    getPersonalizedFeed,
    updateUserPreferences,
    sessionRepository
)

fun AppContainer.createMarketplaceViewModel() = MarketplaceViewModel(
    searchMarketplace,
    MarketplaceBookmarkDependencies(
        add = bookmarkMarketplaceListing,
        remove = removeMarketplaceListingBookmark,
        get = getMarketplaceListingBookmarks
    ),
    viewMarketplaceListing,
    submitMarketplaceReview,
    listingChangedSignal,
    createCurrentUserIdProvider(authRepository)
)

fun AppContainer.createMarketplaceUserReportViewModel() = MarketplaceUserReportViewModel(reportMarketplaceUser)

val AppContainer.marketplaceUserBlockViewModelFactory: () -> MarketplaceUserBlockViewModel
    get() = { MarketplaceUserBlockViewModel(blockMarketplaceUser) }

val AppContainer.blockedUsersViewModelFactory: () -> BlockedUsersViewModel
    get() = { BlockedUsersViewModel(getBlockedUsers, unblockUser, listingChangedSignal) }

fun AppContainer.createSellerProfileViewModel() = SellerProfileViewModel(viewMarketplaceSellerProfile)

fun AppContainer.createListingsViewModel() = ListingsViewModel(
    createListing,
    manageListing,
    markListingSold,
    recentListingBuyersProvider,
    restoreListingToMarketplace,
    deleteListing,
    getSellerListings,
    ListingsViewModelSessionDependencies(
        currentListingSellerProvider,
        listingChangedSignal,
        manageListingImages
    )
)

fun AppContainer.createMessagesViewModel() = MessagesViewModel(
    inboxStateOwner,
    getConversationMessages,
    observeConversationMessages,
    sendMessage,
    reportMessage,
    hideConversation
)

fun AppContainer.createSignInViewModel() = SignInViewModel(signInUser, sessionRepository)

fun AppContainer.createRegistrationViewModel() = RegistrationViewModel(
    registerStudent,
    verifyStudentEmail,
    resendVerificationCode,
    sessionRepository
)

fun AppContainer.createProfileViewModel() = ProfileViewModel(
    sessionRepository,
    getProfileOverview,
    getProfileActivity,
    listingChangedSignal,
    profileSoldListingsProvider,
    ProfileBookmarksDependencies(
        provider = profileBookmarkedListingsProvider,
        remover = profileBookmarkedListingRemover
    )
)

fun AppContainer.createEditProfileViewModel() = EditProfileViewModel(
    sessionRepository,
    updateStudentProfile,
    uploadProfilePhoto,
    userMessenger
)

fun AppContainer.createReviewViewModel() = ReviewViewModel(sessionRepository, publishStudentReview)

fun AppContainer.createAccountViewModel() = AccountViewModel(
    sessionRepository,
    signOutUser,
    softDeleteProfile,
    userMessenger
)

fun AppContainer.createPasswordRecoveryViewModel() = PasswordRecoveryViewModel(
    requestPasswordReset,
    verifyPasswordResetCode,
    updatePassword,
    signOutUser
)

fun AppContainer.createThemeViewModel() = com.jdrms.bulletin.app.theme.ThemeViewModel(themePreferenceStore)
