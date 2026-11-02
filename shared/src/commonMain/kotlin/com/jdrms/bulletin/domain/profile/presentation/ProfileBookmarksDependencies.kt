package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListingRemover
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListingsProvider

data class ProfileBookmarksDependencies(
    val provider: ProfileBookmarkedListingsProvider? = null,
    val remover: ProfileBookmarkedListingRemover? = null
)
