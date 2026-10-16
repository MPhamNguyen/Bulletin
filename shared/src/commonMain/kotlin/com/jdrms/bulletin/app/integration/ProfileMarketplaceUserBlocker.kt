package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceUserBlocker
import com.jdrms.bulletin.domain.profile.application.BlockUser
import com.jdrms.bulletin.domain.profile.domain.model.UserId

class ProfileMarketplaceUserBlocker(
    private val blockUser: BlockUser
) : MarketplaceUserBlocker {
    override suspend fun block(userId: String): Result<Unit> = blockUser(UserId(userId))
}
