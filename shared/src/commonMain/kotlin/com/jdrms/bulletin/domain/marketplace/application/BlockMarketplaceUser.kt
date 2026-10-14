package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.core.common.Result

fun interface MarketplaceUserBlocker {
    suspend fun block(userId: String): Result<Unit>
}

class BlockMarketplaceUser(
    private val blocker: MarketplaceUserBlocker
) {
    suspend operator fun invoke(userId: String): Result<Unit> = blocker.block(userId)
}

fun interface MarketplaceBlockedUserProvider {
    suspend fun getHiddenUserIds(viewerId: String): Set<String>
}
