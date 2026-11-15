package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceInterestIdentity
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterestOwnerId
import com.jdrms.bulletin.domain.profile.application.SessionRepository
import com.jdrms.bulletin.domain.profile.application.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Translates the app session into the marketplace context's interest owner identity. */
class AuthMarketplaceInterestIdentity(
    private val sessionRepository: SessionRepository
) : MarketplaceInterestIdentity {
    override fun observeOwnerIds(): Flow<MarketplaceInterestOwnerId?> =
        sessionRepository.state.map(SessionState::toMarketplaceInterestOwnerId)
}

private fun SessionState.toMarketplaceInterestOwnerId(): MarketplaceInterestOwnerId? =
    (this as? SessionState.Authenticated)?.profile?.id?.value?.let(::MarketplaceInterestOwnerId)
