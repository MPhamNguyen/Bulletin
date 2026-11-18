package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.IllegalReviewRequestTransitionException
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestNotFoundException
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.ReviewRequestDto
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SupabaseReviewRequestConditionalUpdateTest {
    @Test
    fun submissionIsOneConditionalUpdateThatReturnsTheRow() = runTest {
        val table = tableWith("PENDING")

        val result = table.repository().updateStatus(LISTING, BUYER, ReviewRequestStatus.SUBMITTED)

        assertIs<Result.Success<Unit>>(result)
        val request = table.requests.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals(setOf("PENDING", "ACCEPTED"), table.statusFilter(request))
        assertEquals("eq.$LISTING", request.url.parameters["listing_id"])
        assertEquals("eq.$BUYER", request.url.parameters["buyer_id"])
        assertTrue(request.headers["Prefer"].orEmpty().contains("return=representation"))
        assertEquals("SUBMITTED", table.statusOf(LISTING, BUYER))
    }

    @Test
    fun acceptanceLandingBeforeSubmissionStillSucceedsInOneRequest() = runTest {
        val table = tableWith("PENDING")
        table.beforePatch = { table.setStatus("ACCEPTED") }

        val result = table.repository().updateStatus(LISTING, BUYER, ReviewRequestStatus.SUBMITTED)

        assertIs<Result.Success<Unit>>(result)
        assertEquals(1, table.requests.size, "No follow-up read is needed when the update returns a row")
        assertEquals("SUBMITTED", table.statusOf(LISTING, BUYER))
    }

    @Test
    fun cancellationLandingBeforeSubmissionIsReportedAfterOneResolvingRead() = runTest {
        val table = tableWith("PENDING")
        table.beforePatch = { table.setStatus("CANCELLED") }

        val result = table.repository().updateStatus(LISTING, BUYER, ReviewRequestStatus.SUBMITTED)

        val error = assertIs<Result.Error>(result).exception
        assertIs<IllegalReviewRequestTransitionException>(error)
        assertEquals(listOf(HttpMethod.Patch, HttpMethod.Get), table.requests.map { it.method })
        assertEquals("CANCELLED", table.statusOf(LISTING, BUYER))
    }

    @Test
    fun cancellationLandingBeforeAcceptanceIsRejectedAndNotOverwritten() = runTest {
        val table = tableWith("PENDING")
        table.beforePatch = { table.setStatus("CANCELLED") }

        val result = table.repository().updateStatus(LISTING, BUYER, ReviewRequestStatus.ACCEPTED)

        assertIs<IllegalReviewRequestTransitionException>(assertIs<Result.Error>(result).exception)
        assertEquals(setOf("PENDING"), table.statusFilter(table.patches().single()))
        assertEquals("CANCELLED", table.statusOf(LISTING, BUYER))
    }

    @Test
    fun concurrentDuplicateAcceptanceIsAnIdempotentSuccess() = runTest {
        val table = tableWith("PENDING")
        table.beforePatch = { table.setStatus("ACCEPTED") }

        val result = table.repository().updateStatus(LISTING, BUYER, ReviewRequestStatus.ACCEPTED)

        assertIs<Result.Success<Unit>>(result)
        assertEquals(listOf(HttpMethod.Patch, HttpMethod.Get), table.requests.map { it.method })
    }

    @Test
    fun repeatedSubmissionReadsOnlyToResolveTheZeroRowUpdate() = runTest {
        val table = tableWith("SUBMITTED")

        val result = table.repository().updateStatus(LISTING, BUYER, ReviewRequestStatus.SUBMITTED)

        assertIs<Result.Success<Unit>>(result)
        assertEquals(listOf(HttpMethod.Patch, HttpMethod.Get), table.requests.map { it.method })
        assertEquals("SUBMITTED", table.statusOf(LISTING, BUYER))
    }

    @Test
    fun missingRequestIsReportedAfterOneResolvingRead() = runTest {
        val table = FakeReviewRequestTable()

        val result = table.repository().updateStatus(LISTING, BUYER, ReviewRequestStatus.SUBMITTED)

        assertIs<ReviewRequestNotFoundException>(assertIs<Result.Error>(result).exception)
        assertEquals(listOf(HttpMethod.Patch, HttpMethod.Get), table.requests.map { it.method })
    }

    @Test
    fun buyersCannotSetPendingOrCancelledAndNothingIsSent() = runTest {
        val table = tableWith("PENDING")

        listOf(ReviewRequestStatus.PENDING, ReviewRequestStatus.CANCELLED).forEach {
            assertIs<IllegalArgumentException>(
                assertIs<Result.Error>(table.repository().updateStatus(LISTING, BUYER, it)).exception
            )
        }
        assertEquals(0, table.requests.size)
    }

    @Test
    fun restoreCancelsWithOneConditionalUpdateAndLeavesTerminalRowsAlone() = runTest {
        val table = tableWith("ACCEPTED")
        table.rows += ReviewRequestDto(LISTING, "buyer-2", SELLER, "Textbook", "SUBMITTED")

        val result = table.repository().cancelForRestoredListing("listing:$LISTING", SELLER)

        assertIs<Result.Success<Unit>>(result)
        assertEquals(setOf("PENDING", "ACCEPTED"), table.statusFilter(table.patches().single()))
        assertEquals("CANCELLED", table.statusOf(LISTING, BUYER))
        assertEquals("SUBMITTED", table.statusOf(LISTING, "buyer-2"))
    }

    private fun tableWith(status: String) = FakeReviewRequestTable().also {
        it.rows += ReviewRequestDto(LISTING, BUYER, SELLER, "Textbook", status)
        it.requests.clear()
    }

    private fun FakeReviewRequestTable.setStatus(status: String) {
        rows[0] = rows[0].copy(status = status)
    }

    private companion object {
        const val LISTING = "listing-1"
        const val BUYER = "buyer-1"
        const val SELLER = "seller-1"
    }
}
