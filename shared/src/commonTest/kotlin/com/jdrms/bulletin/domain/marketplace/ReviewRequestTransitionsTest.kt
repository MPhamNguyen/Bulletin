package com.jdrms.bulletin.domain.marketplace

import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus.ACCEPTED
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus.CANCELLED
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus.DECLINED
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus.PENDING
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus.SUBMITTED
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestTransition
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestTransitions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ReviewRequestTransitionsTest {
    private val expectedLegal = setOf(
        PENDING to ACCEPTED,
        PENDING to DECLINED,
        PENDING to SUBMITTED,
        PENDING to CANCELLED,
        ACCEPTED to SUBMITTED,
        ACCEPTED to CANCELLED
    )

    @Test
    fun tableContainsExactlyTheLegalTransitions() {
        val actual = ReviewRequestStatus.entries.flatMap { from ->
            ReviewRequestStatus.entries.filter { to -> ReviewRequestTransitions.isLegal(from, to) }
                .map { to -> from to to }
        }.toSet()
        assertEquals(expectedLegal, actual)
    }

    @Test
    fun pendingMaySubmitWithoutAccepting() {
        assertTrue(ReviewRequestTransitions.isLegal(PENDING, SUBMITTED))
    }

    @Test
    fun declinedSubmittedAndCancelledAreTerminal() {
        assertEquals(
            setOf(DECLINED, SUBMITTED, CANCELLED),
            ReviewRequestStatus.entries.filter {
                ReviewRequestTransitions.isTerminal(it)
            }.toSet()
        )
        assertFalse(ReviewRequestTransitions.isActionableByBuyer(SUBMITTED))
        assertTrue(ReviewRequestTransitions.isActionableByBuyer(ACCEPTED))
    }

    @Test
    fun submissionSourcesAreExactlyPendingAndAccepted() {
        assertEquals(listOf(PENDING, ACCEPTED), ReviewRequestTransitions.sourcesFor(SUBMITTED))
        assertEquals(listOf(PENDING), ReviewRequestTransitions.sourcesFor(ACCEPTED))
        assertEquals(listOf(PENDING), ReviewRequestTransitions.sourcesFor(DECLINED))
        assertEquals(listOf(PENDING, ACCEPTED), ReviewRequestTransitions.sourcesFor(CANCELLED))
    }

    @Test
    fun repeatingABuyerStatusIsAnIdempotentNoOp() {
        listOf(ACCEPTED, DECLINED, SUBMITTED).forEach { status ->
            assertEquals(ReviewRequestTransition.AlreadyApplied, ReviewRequestTransitions.decide(status, status))
        }
        assertIs<ReviewRequestTransition.Rejected>(ReviewRequestTransitions.decide(CANCELLED, CANCELLED))
        assertIs<ReviewRequestTransition.Rejected>(ReviewRequestTransitions.decide(PENDING, PENDING))
    }

    @Test
    fun everythingOutsideTheTableIsRejected() {
        ReviewRequestStatus.entries.forEach { from ->
            ReviewRequestStatus.entries.filter { to -> from != to && (from to to) !in expectedLegal }.forEach { to ->
                assertIs<ReviewRequestTransition.Rejected>(ReviewRequestTransitions.decide(from, to), "$from>$to")
            }
        }
    }
}
