package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterestOwnerId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class MarketplaceInterestListingEvent(
    val ownerId: MarketplaceInterestOwnerId,
    val listingId: MarketplaceItemId
)

class MarketplaceInterestListingEvents {
    private val mutableEvents = MutableSharedFlow<MarketplaceInterestListingEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<MarketplaceInterestListingEvent> = mutableEvents.asSharedFlow()

    suspend fun publish(event: MarketplaceInterestListingEvent) {
        mutableEvents.emit(event)
    }
}
