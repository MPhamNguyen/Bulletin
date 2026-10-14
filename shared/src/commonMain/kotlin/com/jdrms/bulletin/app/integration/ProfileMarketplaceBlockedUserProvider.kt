package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceBlockedUserProvider
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.UserBlockRepository

class ProfileMarketplaceBlockedUserProvider(
    private val blockRepository: UserBlockRepository
) : MarketplaceBlockedUserProvider {
    override suspend fun getHiddenUserIds(viewerId: String): Set<String> {
        return when (val result = blockRepository.getHiddenUserIds(UserId(viewerId))) {
            is com.jdrms.bulletin.core.common.Result.Success -> result.data.map(UserId::value).toSet()
            is com.jdrms.bulletin.core.common.Result.Error -> throw result.exception
        }
    }
}
