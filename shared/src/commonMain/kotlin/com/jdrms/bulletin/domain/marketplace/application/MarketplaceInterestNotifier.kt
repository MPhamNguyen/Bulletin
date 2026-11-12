package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterestOwnerId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId

data class MarketplaceInterestAlert(
    val listingId: MarketplaceItemId,
    val recipientId: MarketplaceInterestOwnerId,
    val title: String,
    val summary: String,
    val createdAtMillis: Long
)

interface MarketplaceInterestNotifier {
    fun show(alert: MarketplaceInterestAlert)
    fun clearAll()
}

object NoOpMarketplaceInterestNotifier : MarketplaceInterestNotifier {
    override fun show(alert: MarketplaceInterestAlert) = Unit
    override fun clearAll() = Unit
}
