package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.domain.listings.application.ListingSeller

internal data class ListingActionCallbacks(
    val listingChangedSignal: RefreshSignal?,
    val notify: (String) -> Unit,
    val reload: (ListingSeller?) -> Unit,
    val cancelNotification: () -> Unit = {}
)
