package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
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
            initialProfiles = mapOf(
                "seller" to sellerProfile(),
                "buyer" to reviewerProfile()
            ),
            initialReviews = mapOf("seller" to listOf(sellerReview()))
        )

        val profile = ProfileMarketplaceSellerNameProvider(repository).getSellerProfile("seller")

        assertEquals(1, profile?.reviewCount)
        assertEquals(5.0, profile?.reputationScore)
        assertEquals("Helpful and on time.", profile?.reviews?.single()?.comment)
        assertEquals("Campus Buyer", profile?.reviews?.single()?.reviewerName)
        assertEquals("https://example.com/buyer.png", profile?.reviews?.single()?.reviewerAvatarUrl)
    }

    @Test
    fun sellerProfileUsesReviewersProfileNameWhenReviewHasFallbackName() = runTest {
        val repository = InMemoryProfileRepository(
            initialProfiles = mapOf(
                "seller" to sellerProfile(),
                "buyer" to reviewerProfile()
            ),
            initialReviews = mapOf("seller" to listOf(sellerReview().copy(reviewerName = "Student")))
        )

        val profile = ProfileMarketplaceSellerNameProvider(repository).getSellerProfile("seller")

        assertEquals("Campus Buyer", profile?.reviews?.single()?.reviewerName)
    }

    @Test
    fun sellerProfileUsesFullNameForReviewerUuid() = runTest {
        val reviewerId = "aa12c776-d77c-4c2d-84a4-71e787baf6f8"
        val repository = InMemoryProfileRepository(
            initialProfiles = mapOf(
                "seller" to sellerProfile(),
                reviewerId to reviewerProfile().copy(id = reviewerId)
            ),
            initialReviews = mapOf(
                "seller" to listOf(
                    sellerReview().copy(reviewerId = reviewerId, reviewerName = "Reviewer")
                )
            )
        )

        val profile = ProfileMarketplaceSellerNameProvider(repository).getSellerProfile("seller")

        assertEquals("Campus Buyer", profile?.reviews?.single()?.reviewerName)
    }

    @Test
    fun sellerProfileUsesReviewNameWhenProfileNameIsMissing() = runTest {
        val repository = InMemoryProfileRepository(
            initialProfiles = mapOf(
                "seller" to sellerProfile(),
                "buyer" to reviewerProfile().copy(fullName = null)
            ),
            initialReviews = mapOf("seller" to listOf(sellerReview().copy(reviewerName = "Actual Buyer Name")))
        )

        val profile = ProfileMarketplaceSellerNameProvider(repository).getSellerProfile("seller")

        assertEquals("Actual Buyer Name", profile?.reviews?.single()?.reviewerName)
    }

    @Test
    fun sellerProfileResolvesLegacyEmailReviewerIdentityToItsProfileName() = runTest {
        val repository = InMemoryProfileRepository(
            initialProfiles = mapOf(
                "seller" to sellerProfile(),
                "buyer" to reviewerProfile()
            ),
            initialReviews = mapOf(
                "seller" to listOf(
                    sellerReview().copy(reviewerId = "buyer@campus.edu", reviewerName = "Student")
                )
            )
        )

        val profile = ProfileMarketplaceSellerNameProvider(repository).getSellerProfile("seller")

        assertEquals("Campus Buyer", profile?.reviews?.single()?.reviewerName)
    }

    @Test
    fun sellerProfileLoadsOnlyThreeReviewsAndReturnsNextPageOnDemand() = runTest {
        val repository = InMemoryProfileRepository(
            initialProfiles = mapOf("seller" to sellerProfile(), "buyer" to reviewerProfile()),
            initialReviews = mapOf(
                "seller" to (1..5).map { index ->
                    sellerReview().copy(
                        id = "review-$index",
                        comment = "Review $index",
                        createdAt = "2026-10-${(20 + index).toString().padStart(2, '0')}T12:00:00Z"
                    )
                }
            )
        )
        val provider = ProfileMarketplaceSellerNameProvider(repository)

        val profile = provider.getSellerProfile("seller")
        val nextPage = provider.getSellerReviewsPage(
            "seller",
            profile?.reviewNextOffset ?: error("Missing page cursor"),
            2
        )

        assertEquals(5, profile?.reviewCount)
        assertEquals(3, profile?.reviews?.size)
        assertEquals(3, profile?.reviewNextOffset)
        assertEquals(listOf("Review 2", "Review 1"), nextPage.reviews.map { it.comment })
        assertEquals(null, nextPage.nextOffset)
    }

    @Test
    fun sellerVerificationUsesConfirmedStatusRatherThanTheEmailSuffix() = runTest {
        val profiles = InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = emptyMap())
        val provider = ProfileMarketplaceSellerNameProvider(profiles)
        for ((email, verified) in listOf("student@school.edu" to false, "student@outlook.com" to true)) {
            val profile = StudentProfile(
                UserId("test-user"),
                StudentEmail(email),
                "Test Student",
                isVerified = verified
            )
            profiles.updateProfile(profile)
            assertEquals(verified, provider.getSellerProfile(profile.id.value)?.isVerified)
        }
    }
}

private fun sellerProfile() = ProfileDto(
    id = "seller",
    email = "seller@campus.edu",
    fullName = "Campus Seller",
    university = "Campus University"
)

private fun reviewerProfile() = ProfileDto(
    id = "buyer",
    email = "buyer@campus.edu",
    fullName = "Campus Buyer",
    avatarUrl = "https://example.com/buyer.png"
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
