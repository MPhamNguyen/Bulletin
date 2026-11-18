package com.jdrms.bulletin.domain.profile.infrastructure.repository

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SupabaseProfileReviewPageTest {
    @Test
    fun pageResolvesOnlyVisibleAuthorsAndPreservesTheCursor() = runTest {
        val fixture = ProfileReviewRepositoryFixture(
            rows = listOf(reviewRow(), reviewRow("review-2"), reviewRow("lookahead", SECOND_REVIEWER_ID))
        )
        try {
            val page = fixture.repository.getReviewsPage(revieweeId, offset = 3, pageSize = 2)
            assertEquals(listOf("review-1", "review-2"), page.reviews.map { it.id.value })
            assertEquals(listOf("Campus Buyer", "Campus Buyer"), page.reviews.map { it.reviewerName })
            assertEquals(5, page.nextOffset)
            val query = fixture.requests.first().url.parameters
            assertEquals("eq.${revieweeId.value}", query["reviewee_id"])
            assertEquals("created_at.desc.nullslast,id.desc.nullslast", query["order"])
            assertEquals("3", query["offset"])
            assertEquals("3", query["limit"])
            assertEquals("in.($FIRST_REVIEWER_ID)", fixture.requests.last().url.parameters["id"])
            assertEquals(2, fixture.requests.size)
        } finally {
            fixture.client.close()
        }
    }

    @Test
    fun lastPageUsesFallbackForUnavailableAuthor() = runTest {
        val fixture = ProfileReviewRepositoryFixture(profiles = emptyList())
        try {
            val page = fixture.repository.getReviewsPage(revieweeId, 0, 3)
            assertEquals("Student", page.reviews.single().reviewerName)
            assertNull(page.nextOffset)
        } finally {
            fixture.client.close()
        }
    }

    @Test
    fun emptyPageDoesNotRequestProfiles() = runTest {
        val fixture = ProfileReviewRepositoryFixture(rows = emptyList())
        try {
            val page = fixture.repository.getReviewsPage(revieweeId, 0, 3)
            assertTrue(page.reviews.isEmpty())
            assertNull(page.nextOffset)
            assertEquals(1, fixture.requests.size)
        } finally {
            fixture.client.close()
        }
    }

    @Test
    fun pageLoadPropagatesReviewAndProfileFailures() = runTest {
        for (table in listOf("reviews", "profiles")) {
            val fixture = ProfileReviewRepositoryFixture(failedTable = table)
            try {
                assertFailsWith<Exception> { fixture.repository.getReviewsPage(revieweeId, 0, 3) }
            } finally {
                fixture.client.close()
            }
        }
    }
}
