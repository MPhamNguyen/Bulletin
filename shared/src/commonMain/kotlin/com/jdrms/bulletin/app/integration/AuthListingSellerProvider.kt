package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.AuthenticationRequiredException
import com.jdrms.bulletin.domain.listings.application.CurrentListingSellerProvider
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.application.ListingSellerLookupException
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.profile.domain.repository.AuthRepository

class AuthListingSellerProvider(
    private val authRepository: AuthRepository
) : CurrentListingSellerProvider {
    override suspend fun getCurrentSeller(): Result<ListingSeller> {
        return when (val result = authRepository.getCurrentUser()) {
            is Result.Success -> {
                val profile = result.data
                if (profile == null) {
                    Result.Error(AuthenticationRequiredException())
                } else {
                    Result.Success(ListingSeller(SellerId(profile.id.value), profile.fullName))
                }
            }
            is Result.Error -> Result.Error(ListingSellerLookupException(result.exception))
        }
    }
}
