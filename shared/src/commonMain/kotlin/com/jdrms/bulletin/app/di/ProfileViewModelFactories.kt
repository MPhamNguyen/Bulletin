package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.app.integration.ListingsActiveListingsCountProvider
import com.jdrms.bulletin.app.integration.ListingsSoldListingsCountProvider
import com.jdrms.bulletin.app.integration.MarketplaceProfileBookmarkedListingsProvider
import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.common.UserMessenger
import com.jdrms.bulletin.domain.profile.application.GetProfileActivity
import com.jdrms.bulletin.domain.profile.application.GetProfileOverview
import com.jdrms.bulletin.domain.profile.presentation.AccountViewModel
import com.jdrms.bulletin.domain.profile.presentation.BlockedUsersViewModel
import com.jdrms.bulletin.domain.profile.presentation.EditProfileViewModel
import com.jdrms.bulletin.domain.profile.presentation.PasswordRecoveryViewModel
import com.jdrms.bulletin.domain.profile.presentation.ProfileBookmarksDependencies
import com.jdrms.bulletin.domain.profile.presentation.ProfileViewModel
import com.jdrms.bulletin.domain.profile.presentation.RegistrationViewModel
import com.jdrms.bulletin.domain.profile.presentation.ReviewViewModel
import com.jdrms.bulletin.domain.profile.presentation.SignInViewModel

internal fun ProfileModule.createBlockedUsersViewModel(listingChangedSignal: RefreshSignal) =
    BlockedUsersViewModel(getBlockedUsers, unblockUser, listingChangedSignal)

internal fun ProfileModule.createSignInViewModel() = SignInViewModel(signInUser, sessionRepository, signInExternally)

internal fun ProfileModule.createRegistrationViewModel() = RegistrationViewModel(
    registerStudent,
    verifyStudentEmail,
    resendVerificationCode,
    sessionRepository
)

internal fun ProfileModule.createProfileViewModel(
    listings: ListingsModule,
    marketplace: MarketplaceModule,
    listingChangedSignal: RefreshSignal
): ProfileViewModel {
    val activeListings = ListingsActiveListingsCountProvider(listings.listingsRepository)
    val bookmarks = MarketplaceProfileBookmarkedListingsProvider(
        marketplace.getMarketplaceListingBookmarks,
        marketplace.removeMarketplaceListingBookmark,
        listings.listingsRepository
    )
    return ProfileViewModel(
        sessionRepository,
        GetProfileOverview(profileRepository, activeListings),
        GetProfileActivity(activeListings),
        listingChangedSignal,
        ListingsSoldListingsCountProvider(listings.listingsRepository),
        ProfileBookmarksDependencies(provider = bookmarks, remover = bookmarks)
    )
}

internal fun ProfileModule.createEditProfileViewModel(userMessenger: UserMessenger) = EditProfileViewModel(
    sessionRepository,
    updateStudentProfile,
    uploadProfilePhoto,
    userMessenger
)

internal fun ProfileModule.createReviewViewModel() = ReviewViewModel(sessionRepository, publishStudentReview)

internal fun ProfileModule.createAccountViewModel(userMessenger: UserMessenger) = AccountViewModel(
    sessionRepository,
    signOutUser,
    softDeleteProfile,
    userMessenger
)

internal fun ProfileModule.createPasswordRecoveryViewModel() = PasswordRecoveryViewModel(
    requestPasswordReset,
    verifyPasswordResetCode,
    updatePassword,
    signOutUser
)
