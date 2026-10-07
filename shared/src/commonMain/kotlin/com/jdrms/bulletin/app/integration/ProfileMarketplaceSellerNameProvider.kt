package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerProfile
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerProfileProvider
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository

class ProfileMarketplaceSellerNameProvider(
    private val profileRepository: ProfileRepository
) : MarketplaceSellerProfileProvider {
    override suspend fun getSellerProfile(sellerId: String): MarketplaceSellerProfile? {
        return when (val result = profileRepository.getProfile(UserId(sellerId))) {
            is com.jdrms.bulletin.core.common.Result.Success -> result.data?.let {
                MarketplaceSellerProfile(
                    sellerId = it.id.value,
                    name = it.fullName,
                    school = it.university,
                    major = it.major,
                    graduationDate = it.graduationDate,
                    bio = it.bio,
                    avatarUrl = it.avatarUrl,
                    reputationScore = it.reputation?.averageRating,
                    reviewCount = it.reputation?.totalReviews,
                    isVerified = it.isVerified || it.email.isUniversityEmail
                )
            }
            is com.jdrms.bulletin.core.common.Result.Error -> null
        }
    }
}
