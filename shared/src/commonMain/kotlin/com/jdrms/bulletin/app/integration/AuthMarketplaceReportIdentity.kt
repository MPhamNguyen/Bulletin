package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceReportIdentity
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository

class AuthMarketplaceReportIdentity(
    private val authRepository: AuthRepository
) : MarketplaceReportIdentity {
    override suspend fun currentReporterId(): Result<MarketplaceReporterId?> =
        when (val result = authRepository.getCurrentUserId()) {
            is Result.Success -> Result.Success(result.data?.let { MarketplaceReporterId(it.value) })
            is Result.Error -> result
        }
}
