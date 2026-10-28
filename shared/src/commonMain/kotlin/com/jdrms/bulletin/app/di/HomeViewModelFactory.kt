package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.domain.home.presentation.HomeViewModel
import com.jdrms.bulletin.domain.profile.application.SessionRepository

internal fun HomeModule.createHomeViewModel(
    sessionRepository: SessionRepository,
    listingChangedSignal: RefreshSignal
) = HomeViewModel(
    getPersonalizedFeed,
    updateUserPreferences,
    sessionRepository,
    listingChangedSignal,
    swipeDecisionRepository,
    recordSwipeDecision,
    sendQuickMessage,
    homeSaveListing
)
