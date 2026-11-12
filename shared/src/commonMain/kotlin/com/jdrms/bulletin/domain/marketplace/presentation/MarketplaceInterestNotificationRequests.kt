package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterestOwnerId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class MarketplaceInterestNotificationRequest(
    val listingId: MarketplaceItemId,
    val recipientId: MarketplaceInterestOwnerId
)

internal class MarketplaceInterestNotificationRequests {
    private val mutablePending = MutableStateFlow<MarketplaceInterestNotificationRequest?>(null)
    val pending: StateFlow<MarketplaceInterestNotificationRequest?> = mutablePending.asStateFlow()

    fun submit(request: MarketplaceInterestNotificationRequest) {
        mutablePending.value = request
    }

    fun take(viewerId: MarketplaceInterestOwnerId?): MarketplaceItemId? {
        val request = mutablePending.value ?: return null
        if (viewerId == null) return null
        mutablePending.value = null
        return request.listingId.takeIf { request.recipientId == viewerId }
    }
}
