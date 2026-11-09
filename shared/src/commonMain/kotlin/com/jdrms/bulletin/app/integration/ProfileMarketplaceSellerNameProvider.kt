package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerProfile
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerProfileProvider
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerReview
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerReviewPage
import com.jdrms.bulletin.domain.profile.domain.model.StudentReputation
import com.jdrms.bulletin.domain.profile.domain.model.StudentReview
import com.jdrms.bulletin.domain.profile.domain.model.StudentReviewPage
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository

class ProfileMarketplaceSellerNameProvider(
    private val profileRepository: ProfileRepository
) : MarketplaceSellerProfileProvider {
    override suspend fun getSellerProfile(sellerId: String): MarketplaceSellerProfile? {
        val sellerIdValue = UserId(sellerId)
        val summary = runCatching { profileRepository.getReputationSummary(sellerIdValue) }
            .getOrDefault(StudentReputation(sellerIdValue))
        val firstPage = runCatching { profileRepository.getReviewsPage(sellerIdValue, 0, PROFILE_PREVIEW_REVIEW_COUNT) }
            .getOrNull() ?: StudentReviewPage(emptyList(), null)
        return when (val result = profileRepository.getProfileBasics(sellerIdValue)) {
            is com.jdrms.bulletin.core.common.Result.Success -> result.data?.let {
                MarketplaceSellerProfile(
                    sellerId = it.id.value,
                    name = it.fullName,
                    school = it.university,
                    major = it.major,
                    bio = it.bio,
                    avatarUrl = it.avatarUrl,
                    reputationScore = summary.averageRating,
                    reviewCount = summary.totalReviews,
                    reviews = mapReviews(firstPage.reviews),
                    reviewNextOffset = firstPage.nextOffset,
                    isVerified = it.isVerified
                )
            }
            is com.jdrms.bulletin.core.common.Result.Error -> null
        }
    }

    override suspend fun getSellerReviewsPage(
        sellerId: String,
        offset: Int,
        pageSize: Int
    ): MarketplaceSellerReviewPage {
        val page = profileRepository.getReviewsPage(UserId(sellerId), offset, pageSize)
        return MarketplaceSellerReviewPage(mapReviews(page.reviews), page.nextOffset)
    }

    private suspend fun mapReviews(reviews: List<StudentReview>): List<MarketplaceSellerReview> =
        reviews.map { review ->
            val reviewer = when (
                val result = profileRepository.getPublicIdentity(UserId(review.reviewerId))
            ) {
                is com.jdrms.bulletin.core.common.Result.Success -> result.data
                is com.jdrms.bulletin.core.common.Result.Error -> null
            }
            MarketplaceSellerReview(
                id = review.id.value,
                reviewerName = resolveReviewerName(review, reviewer?.fullName),
                rating = review.rating.score,
                comment = review.comment,
                createdAtMillis = review.createdAtMillis,
                reviewerAvatarUrl = reviewer?.avatarUrl,
                relatedListingId = review.relatedListingId
            )
        }

    private fun resolveReviewerName(review: StudentReview, profileName: String?): String =
        profileName?.takeUnless(::isPlaceholderReviewerName)?.takeIf(String::isNotBlank)
            ?: review.reviewerName.takeUnless(::isPlaceholderReviewerName)?.takeIf(String::isNotBlank)
            ?: FALLBACK_REVIEWER_NAME

    private fun isPlaceholderReviewerName(name: String): Boolean = name in PLACEHOLDER_REVIEWER_NAMES

    private companion object {
        const val PROFILE_PREVIEW_REVIEW_COUNT = 3
        const val FALLBACK_REVIEWER_NAME = "Reviewer"
        val PLACEHOLDER_REVIEWER_NAMES = setOf("Student", FALLBACK_REVIEWER_NAME)
    }
}
