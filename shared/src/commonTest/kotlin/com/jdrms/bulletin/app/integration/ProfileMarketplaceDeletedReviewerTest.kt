package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.PublicProfileIdentity
import com.jdrms.bulletin.domain.profile.domain.model.StudentReviewPage
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ProfileDto
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ReviewDto
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProfileMarketplaceDeletedReviewerTest {
    @Test
    fun sellerPreviewAnonymizesDeletedReviewerAndKeepsTheirRating() = runTest {
        val profile = ProfileMarketplaceSellerNameProvider(deletedReviewerRepository()).getSellerProfile("seller")
        val review = profile?.reviews?.single() ?: error("Missing retained review")

        assertEquals("Student", review.reviewerName)
        assertNull(review.reviewerAvatarUrl)
        assertNull(review.relatedListingId)
        assertEquals(4, review.rating)
        assertEquals("", review.comment)
        assertEquals(1, profile.reviewCount)
        assertEquals(4.0, profile.reputationScore)
    }

    @Test
    fun sellerReviewPageDoesNotLookUpDeletedReviewerOrRestoreTheirOldName() = runTest {
        val page = ProfileMarketplaceSellerNameProvider(deletedReviewerRepository())
            .getSellerReviewsPage("seller", 0, 3)

        assertEquals("Student", page.reviews.single().reviewerName)
        assertNull(page.reviews.single().reviewerAvatarUrl)
        assertNull(page.nextOffset)
    }
}

private fun deletedReviewerRepository(): ProfileRepository {
    val memory = InMemoryProfileRepository(
        initialProfiles = mapOf("seller" to ProfileDto("seller", "seller@campus.edu", fullName = "Seller")),
        initialReviews = mapOf(
            "seller" to listOf(
                ReviewDto(
                    id = "retained-review", reviewerId = null, reviewerName = "Former author",
                    revieweeId = "seller", score = 4, comment = "", wasListingReview = true
                )
            )
        )
    )
    return object : ProfileRepository by memory {
        override suspend fun getPublicIdentity(userId: UserId): Result<PublicProfileIdentity?> =
            error("A deleted reviewer must not cause a profile lookup")

        override suspend fun getReviewsPage(userId: UserId, offset: Int, pageSize: Int): StudentReviewPage {
            val page = memory.getReviewsPage(userId, offset, pageSize)
            return page.copy(reviews = page.reviews.map { it.copy(reviewerName = "Stale cached author") })
        }
    }
}
