package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterestOwnerId
import kotlinx.coroutines.flow.Flow

fun interface MarketplaceInterestIdentity {
    fun observeOwnerIds(): Flow<MarketplaceInterestOwnerId?>
}
