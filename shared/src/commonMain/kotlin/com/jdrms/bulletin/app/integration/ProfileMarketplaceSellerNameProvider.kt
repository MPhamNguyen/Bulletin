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
import kotlinx.coroutines.CancellationException

class ProfileMarketplaceSellerNameProvider(
    private val profileRepository: ProfileRepository
) : MarketplaceSellerProfileProvider {
    override suspend fun getSellerProfile(sellerId: String): MarketplaceSellerProfile? {
        val sellerIdValue = UserId(sellerId)
        val summary = try {
            profileRepository.getReputationSummary(sellerIdValue)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            StudentReputation(sellerIdValue)
        }
        val firstPage = try {
            profileRepository.getReviewsPage(sellerIdValue, 0, PROFILE_PREVIEW_REVIEW_COUNT)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
        val reviewPageLoadFailed = firstPage == null
        val resolvedFirstPage = firstPage ?: StudentReviewPage(emptyList(), null)
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
                    reviews = mapReviews(resolvedFirstPage.reviews),
                    reviewNextOffset = if (reviewPageLoadFailed) 0 else resolvedFirstPage.nextOffset,
                    reviewPageLoadFailed = reviewPageLoadFailed,
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
            val reviewer = review.reviewerId?.let { reviewerId ->
                when (val result = profileRepository.getPublicIdentity(UserId(reviewerId))) {
                    is com.jdrms.bulletin.core.common.Result.Success -> result.data
                    is com.jdrms.bulletin.core.common.Result.Error -> null
                }
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

    private fun resolveReviewerName(review: StudentReview, profileName: String?): String {
        if (review.reviewerId == null) return FALLBACK_REVIEWER_NAME
        return profileName?.trim()?.takeUnless(::isPlaceholderReviewerName)?.takeIf(String::isNotBlank)
            ?: review.reviewerName.trim().takeUnless(::isPlaceholderReviewerName)?.takeIf(String::isNotBlank)
            ?: FALLBACK_REVIEWER_NAME
    }

    private fun isPlaceholderReviewerName(name: String): Boolean = name in PLACEHOLDER_REVIEWER_NAMES

    private companion object {
        const val PROFILE_PREVIEW_REVIEW_COUNT = 3
        const val FALLBACK_REVIEWER_NAME = "Student"
        val PLACEHOLDER_REVIEWER_NAMES = setOf("Reviewer", FALLBACK_REVIEWER_NAME)
    }
}
