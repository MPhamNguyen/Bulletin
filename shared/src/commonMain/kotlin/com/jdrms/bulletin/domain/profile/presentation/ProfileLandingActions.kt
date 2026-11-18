package com.jdrms.bulletin.domain.profile.presentation

data class ProfileLandingActions(
    val onRefresh: () -> Unit,
    val onSettingsClick: () -> Unit,
    val onMyListingsClick: () -> Unit,
    val onBookmarkedListingsClick: () -> Unit,
    val onInterestsClick: () -> Unit,
    val onViewAllReviews: () -> Unit
)
