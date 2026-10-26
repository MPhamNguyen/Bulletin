package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.profile.infrastructure.dto.ProfileDto
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ReviewDto
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileMarketplaceSellerNameProviderTest {
    @Test
    fun sellerProfileIncludesReviewSnapshots() = runTest {
        val repository = InMemoryProfileRepository(
            initialProfiles = mapOf("seller" to sellerProfile()),
            initialReviews = mapOf("seller" to listOf(sellerReview()))
        )

        val profile = ProfileMarketplaceSellerNameProvider(repository).getSellerProfile("seller")

        assertEquals(1, profile?.reviewCount)
        assertEquals(5.0, profile?.reputationScore)
        assertEquals("Helpful and on time.", profile?.reviews?.single()?.comment)
        assertEquals("Campus Buyer", profile?.reviews?.single()?.reviewerName)
    }
}

private fun sellerProfile() = ProfileDto(
    id = "seller",
    email = "seller@campus.edu",
    fullName = "Campus Seller",
    university = "Campus University"
)

private fun sellerReview() = ReviewDto(
    id = "review-1",
    reviewerId = "buyer",
    reviewerName = "Campus Buyer",
    revieweeId = "seller",
    score = 5,
    comment = "Helpful and on time.",
    createdAt = "2026-10-25T12:00:00Z"
)
