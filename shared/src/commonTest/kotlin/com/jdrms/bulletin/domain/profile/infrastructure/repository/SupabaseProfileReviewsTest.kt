package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.domain.profile.infrastructure.dto.PublicProfileIdentityDto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SupabaseProfileReviewsTest {
    @Test
    fun reputationResolvesRawReviewAuthorsByIdInOneBatch() = runTest {
        val rows = listOf(reviewRow(), reviewRow("review-2", SECOND_REVIEWER_ID), reviewRow("review-3"))
        val profiles = listOf(
            PublicProfileIdentityDto(SECOND_REVIEWER_ID, "Second Buyer"),
            PublicProfileIdentityDto(FIRST_REVIEWER_ID, "  Campus Buyer  ")
        )
        val fixture = ProfileReviewRepositoryFixture(rows, profiles)
        try {
            val reputation = fixture.repository.getReputation(revieweeId)
            assertEquals(
                listOf("Campus Buyer", "Second Buyer", "Campus Buyer"),
                reputation.reviews.map { it.reviewerName }
            )
            assertEquals(3, reputation.totalReviews)
            assertEquals(5.0, reputation.averageRating)
            assertEquals(rows.map { it.id }, reputation.reviews.map { it.id.value })
            assertEquals("listing-1", reputation.reviews.first().relatedListingId)
            assertEquals(listOf("/rest/v1/reviews", "/rest/v1/profiles"), fixture.requests.map { it.url.encodedPath })
            assertEquals("eq.${revieweeId.value}", fixture.requests.first().url.parameters["reviewee_id"])
            val query = fixture.requests.last().url.parameters
            assertEquals("id,full_name", query["select"])
            assertEquals("in.($FIRST_REVIEWER_ID,$SECOND_REVIEWER_ID)", query["id"])
        } finally {
            fixture.client.close()
        }
    }

    @Test
    fun resolvedReviewMatchesTheInMemoryRepositoryContract() = runTest {
        val fixture = ProfileReviewRepositoryFixture()
        val memory = InMemoryProfileRepository(
            initialProfiles = emptyMap(),
            initialReviews = mapOf(revieweeId.value to listOf(reviewRow().copy(reviewerName = "Campus Buyer")))
        )
        try {
            assertEquals(memory.getReputation(revieweeId), fixture.repository.getReputation(revieweeId))
            assertEquals(memory.getReviewsPage(revieweeId, 0, 3), fixture.repository.getReviewsPage(revieweeId, 0, 3))
        } finally {
            fixture.client.close()
        }
    }

    @Test
    fun missingNullAndBlankProfileNamesUseReviewerWithoutDroppingReviews() = runTest {
        val fixture = ProfileReviewRepositoryFixture(
            rows = listOf(reviewRow(), reviewRow("review-2", SECOND_REVIEWER_ID), reviewRow("review-3", "missing")),
            profiles = listOf(
                PublicProfileIdentityDto(FIRST_REVIEWER_ID, null),
                PublicProfileIdentityDto(SECOND_REVIEWER_ID, "  ")
            )
        )
        try {
            val reputation = fixture.repository.getReputation(revieweeId)
            assertEquals(listOf("Reviewer", "Reviewer", "Reviewer"), reputation.reviews.map { it.reviewerName })
            assertEquals(3, reputation.totalReviews)
        } finally {
            fixture.client.close()
        }
    }

    @Test
    fun emptyHistoryDoesNotRequestProfiles() = runTest {
        val fixture = ProfileReviewRepositoryFixture(rows = emptyList())
        try {
            assertTrue(fixture.repository.getReputation(revieweeId).reviews.isEmpty())
            assertEquals(listOf("/rest/v1/reviews"), fixture.requests.map { it.url.encodedPath })
        } finally {
            fixture.client.close()
        }
    }

    @Test
    fun failedRequestsAreNotCachedAsEmptyHistories() = runTest {
        for (table in listOf("reviews", "profiles")) {
            val fixture = ProfileReviewRepositoryFixture(failedTable = table)
            try {
                repeat(2) {
                    assertFailsWith<Exception> { fixture.repository.getReputation(revieweeId) }
                }
                assertEquals(2, fixture.requests.count { it.url.encodedPath == "/rest/v1/$table" })
            } finally {
                fixture.client.close()
            }
        }
    }

    @Test
    fun largeHistoriesSplitProfileLookupsIntoBoundedBatches() = runTest {
        val rows = (1..101).map {
            reviewRow("review-$it", "00000000-0000-0000-0000-${it.toString().padStart(12, '0')}")
        }
        val fixture = ProfileReviewRepositoryFixture(rows, rows.map { PublicProfileIdentityDto(it.reviewerId, it.id) })
        try {
            val reputation = fixture.repository.getReputation(revieweeId)
            assertEquals(rows.map { it.id }, reputation.reviews.map { it.reviewerName })
            val queries = fixture.requests.filter { it.url.encodedPath == "/rest/v1/profiles" }
            val batchSizes = queries.map {
                it.url.parameters["id"]!!.removePrefix("in.(").removeSuffix(")").split(',').size
            }
            assertEquals(listOf(100, 1), batchSizes)
        } finally {
            fixture.client.close()
        }
    }
}
