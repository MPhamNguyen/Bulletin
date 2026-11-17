package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.domain.profile.infrastructure.dto.PublicProfileIdentityDto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SupabaseDeletedReviewerTest {
    @Test
    fun deletedAuthorsRemainInReputationWithoutAnyProfileLookup() = runTest {
        val rows = listOf(
            reviewRow().copy(reviewerId = null, reviewerName = "Former author", listingId = null),
            reviewRow("rating-only").copy(
                reviewerId = null,
                listingId = null,
                score = 3,
                comment = "",
                wasListingReview = true
            )
        )
        val fixture = ProfileReviewRepositoryFixture(rows = rows, failedTable = "profiles")
        val memory =
            InMemoryProfileRepository(initialProfiles = emptyMap(), initialReviews = mapOf(revieweeId.value to rows))
        try {
            val reputation = fixture.repository.getReputation(revieweeId)
            assertEquals(listOf("Student", "Student"), reputation.reviews.map { it.reviewerName })
            assertEquals(2, reputation.totalReviews)
            assertEquals(4.0, reputation.averageRating)
            assertTrue(reputation.reviews.all { it.reviewerId == null })
            assertEquals(memory.getReputation(revieweeId), reputation)
            assertEquals(listOf("/rest/v1/reviews"), fixture.requests.map { it.url.encodedPath })
        } finally {
            fixture.client.close()
        }
    }

    @Test
    fun mixedAuthorPageOnlyLooksUpSurvivingIdsAndKeepsPagination() = runTest {
        val rows = listOf(
            reviewRow("deleted").copy(reviewerId = null, listingId = null),
            reviewRow("live"),
            reviewRow("lookahead", SECOND_REVIEWER_ID)
        )
        val fixture = ProfileReviewRepositoryFixture(rows, listOf(PublicProfileIdentityDto(FIRST_REVIEWER_ID, "Buyer")))
        try {
            val page = fixture.repository.getReviewsPage(revieweeId, offset = 0, pageSize = 2)
            assertEquals(listOf("deleted", "live"), page.reviews.map { it.id.value })
            assertEquals(listOf("Student", "Buyer"), page.reviews.map { it.reviewerName })
            assertNull(page.reviews.first().reviewerId)
            assertNull(page.reviews.first().relatedListingId)
            assertEquals(2, page.nextOffset)
            assertEquals("in.($FIRST_REVIEWER_ID)", fixture.requests.last().url.parameters["id"])
        } finally {
            fixture.client.close()
        }
    }
}
