package com.jdrms.bulletin.domain.marketplace

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.IllegalReviewRequestTransitionException
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestNotFoundException
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus.ACCEPTED
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus.CANCELLED
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus.DECLINED
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus.PENDING
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus.SUBMITTED
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestTransitions
import com.jdrms.bulletin.domain.marketplace.domain.repository.ReviewRequestRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Behaviour every [ReviewRequestRepository] must share. Subclasses supply a fresh, empty repository.
 * Terminal states are only observable through behaviour: they disappear from [pendingForBuyer] and reject
 * every further change.
 */
abstract class ReviewRequestRepositoryContract {
    protected abstract fun newRepository(): ReviewRequestRepository

    @Test
    fun pendingCanBeAcceptedThenSubmitted() = runTest {
        val repository = seeded(PENDING)
        assertSuccess(repository.updateStatus(LISTING, BUYER, ACCEPTED))
        assertEquals(ACCEPTED, repository.pendingForBuyer(BUYER).single().status)
        assertSuccess(repository.updateStatus(LISTING, BUYER, SUBMITTED))
        assertEquals(emptyList(), repository.pendingForBuyer(BUYER))
    }

    @Test
    fun pendingCanBeSubmittedWithoutAcceptance() = runTest {
        val repository = seeded(PENDING)
        assertSuccess(repository.updateStatus(LISTING, BUYER, SUBMITTED))
        assertEquals(emptyList(), repository.pendingForBuyer(BUYER))
    }

    @Test
    fun pendingCanBeDeclined() = runTest {
        val repository = seeded(PENDING)
        assertSuccess(repository.updateStatus(LISTING, BUYER, DECLINED))
        assertEquals(emptyList(), repository.pendingForBuyer(BUYER))
    }

    @Test
    fun acceptedCannotBeDeclinedAndStaysAccepted() = runTest {
        val repository = seeded(ACCEPTED)
        assertIllegal(repository.updateStatus(LISTING, BUYER, DECLINED))
        assertEquals(ACCEPTED, repository.pendingForBuyer(BUYER).single().status)
    }

    @Test
    fun terminalStatesRejectEveryOtherChange() = runTest {
        listOf(DECLINED, SUBMITTED, CANCELLED).forEach { terminal ->
            val repository = seeded(terminal)
            ReviewRequestTransitions.buyerSettable.filter { it != terminal }.forEach { next ->
                assertIllegal(repository.updateStatus(LISTING, BUYER, next), "$terminal -> $next")
            }
            assertEquals(emptyList(), repository.pendingForBuyer(BUYER), "$terminal must stay out of the queue")
        }
    }

    @Test
    fun cancelledRequestRejectsSubmission() = runTest {
        assertIllegal(seeded(CANCELLED).updateStatus(LISTING, BUYER, SUBMITTED))
    }

    @Test
    fun repeatedSubmissionSucceedsAndStaysSubmitted() = runTest {
        val repository = seeded(SUBMITTED)
        assertSuccess(repository.updateStatus(LISTING, BUYER, SUBMITTED))
        assertSuccess(repository.updateStatus(LISTING, BUYER, SUBMITTED))
        assertIllegal(repository.updateStatus(LISTING, BUYER, ACCEPTED))
        assertEquals(emptyList(), repository.pendingForBuyer(BUYER))
    }

    @Test
    fun repeatedAcceptanceAndDeclineAreIdempotent() = runTest {
        val accepted = seeded(ACCEPTED)
        assertSuccess(accepted.updateStatus(LISTING, BUYER, ACCEPTED))
        assertEquals(ACCEPTED, accepted.pendingForBuyer(BUYER).single().status)
        assertSuccess(seeded(DECLINED).updateStatus(LISTING, BUYER, DECLINED))
    }

    @Test
    fun missingRequestIsReportedForEveryBuyerStatus() = runTest {
        val repository = newRepository()
        ReviewRequestTransitions.buyerSettable.forEach { status ->
            val error = assertIs<Result.Error>(repository.updateStatus(LISTING, BUYER, status)).exception
            assertIs<ReviewRequestNotFoundException>(error, "$status")
        }
        seeded(PENDING).let { other ->
            val error = assertIs<Result.Error>(other.updateStatus("other-listing", BUYER, SUBMITTED)).exception
            assertIs<ReviewRequestNotFoundException>(error)
        }
    }

    @Test
    fun buyersCannotSetPendingOrCancelled() = runTest {
        val repository = seeded(PENDING)
        listOf(PENDING, CANCELLED).forEach { status ->
            val error = assertIs<Result.Error>(repository.updateStatus(LISTING, BUYER, status)).exception
            assertIs<IllegalArgumentException>(error, "$status")
        }
        assertEquals(PENDING, repository.pendingForBuyer(BUYER).single().status)
    }

    @Test
    fun listingPrefixIsIgnoredWhenUpdating() = runTest {
        val repository = seeded(PENDING)
        assertSuccess(repository.updateStatus("listing:$LISTING", BUYER, ACCEPTED))
        assertEquals(ACCEPTED, repository.pendingForBuyer(BUYER).single().status)
    }

    @Test
    fun restoreCancelsOnlyActiveRequests() = runTest {
        val repository = seeded(ACCEPTED)
        assertSuccess(repository.cancelForRestoredListing(LISTING, SELLER))
        assertEquals(emptyList(), repository.pendingForBuyer(BUYER))
        assertIllegal(repository.updateStatus(LISTING, BUYER, SUBMITTED))

        val submitted = seeded(SUBMITTED)
        assertSuccess(submitted.cancelForRestoredListing(LISTING, SELLER))
        assertSuccess(submitted.updateStatus(LISTING, BUYER, SUBMITTED))
        assertIllegal(submitted.updateStatus(LISTING, BUYER, DECLINED))
    }

    private suspend fun seeded(status: ReviewRequestStatus): ReviewRequestRepository {
        val repository = newRepository()
        assertSuccess(repository.createIfMissing(ReviewRequest(MarketplaceItemId(LISTING), BUYER, SELLER, "Textbook")))
        when (status) {
            PENDING -> Unit
            CANCELLED -> assertSuccess(repository.cancelForRestoredListing(LISTING, SELLER))
            else -> assertSuccess(repository.updateStatus(LISTING, BUYER, status))
        }
        return repository
    }

    private fun assertSuccess(result: Result<Unit>) {
        assertTrue(result is Result.Success, "Expected success but was $result")
    }

    private fun assertIllegal(result: Result<Unit>, message: String? = null) {
        assertIs<IllegalReviewRequestTransitionException>(assertIs<Result.Error>(result, message).exception, message)
    }

    private companion object {
        const val LISTING = "listing-1"
        const val BUYER = "buyer-1"
        const val SELLER = "seller-1"
    }
}
