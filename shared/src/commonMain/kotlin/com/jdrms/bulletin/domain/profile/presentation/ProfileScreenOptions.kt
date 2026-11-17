package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.runtime.Composable
import com.jdrms.bulletin.app.theme.ThemeViewModel

data class ProfileScreenOptions(
    val interestNotificationCount: Int = 0,
    val themeViewModel: ThemeViewModel? = null,
    val reportedListingVisibilitySetting: ReportedListingVisibilitySetting? = null,
    val profileReviewsContent: @Composable (onViewAllReviews: () -> Unit) -> Unit = {}
)
