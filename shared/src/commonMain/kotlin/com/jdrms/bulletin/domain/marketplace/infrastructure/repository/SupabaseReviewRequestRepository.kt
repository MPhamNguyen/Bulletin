package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.IllegalReviewRequestTransitionException
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestNotFoundException
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestTransition
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestTransitions
import com.jdrms.bulletin.domain.marketplace.domain.repository.ReviewRequestRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.ReviewRequestDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException

class SupabaseReviewRequestRepository(
    private val client: SupabaseClient,
    private val currentUserId: () -> String? = { client.auth.currentUserOrNull()?.id }
) : ReviewRequestRepository {
    override suspend fun createIfMissing(request: ReviewRequest): Result<Unit> = safely {
        require(currentUserId() == request.sellerId) { "Only the seller can create a review request." }
        client.from(TABLE).upsert(
            ReviewRequestDto(
                request.listingId.value.removePrefix("listing:"),
                request.buyerId,
                request.sellerId,
                request.listingTitle,
                ReviewRequestStatus.PENDING.name
            )
        ) {
            onConflict = "listing_id,buyer_id"
            ignoreDuplicates = true
        }
    }

    override suspend fun pendingForBuyer(buyerId: String): List<ReviewRequest> {
        if (currentUserId() != buyerId) return emptyList()
        return client.from(TABLE).select(columns = COLUMNS) {
            filter { eq("buyer_id", buyerId) }
        }.decodeList<ReviewRequestDto>().map(::toDomain).filter { it.isActionableByBuyer }
    }

    override suspend fun updateStatus(
        listingId: String,
        buyerId: String,
        status: ReviewRequestStatus
    ): Result<Unit> = safely {
        require(currentUserId() == buyerId) { "Only the buyer can update a review request." }
        require(status in ReviewRequestTransitions.buyerSettable) {
            "Buyers cannot set this review request status."
        }
        persistStatus(listingId.removePrefix("listing:"), buyerId, status)
    }

    override suspend fun cancelForRestoredListing(listingId: String, sellerId: String): Result<Unit> = safely {
        require(currentUserId() == sellerId) { "Only the seller can cancel review requests." }
        val id = listingId.removePrefix("listing:")
        val cancellable = ReviewRequestTransitions.sourcesFor(ReviewRequestStatus.CANCELLED).map { it.name }
        client.from(TABLE).update({ set("status", ReviewRequestStatus.CANCELLED.name) }) {
            filter {
                eq("listing_id", id)
                eq("seller_id", sellerId)
                isIn("status", cancellable)
            }
        }
        val outstanding = client.from(TABLE).select(columns = COLUMNS) {
            filter {
                eq("listing_id", id)
                eq("seller_id", sellerId)
            }
        }.decodeList<ReviewRequestDto>().map { ReviewRequestStatus.valueOf(it.status) }
            .filter(ReviewRequestTransitions::isActionableByBuyer)
        check(outstanding.isEmpty()) { "Outstanding review requests could not be cancelled." }
    }

    /**
     * One conditional UPDATE ... RETURNING: the row changes only while its current status is a legal source for
     * [status] (for SUBMITTED, PENDING or ACCEPTED), so a concurrent acceptance cannot make an eligible
     * submission fail. A returned row means success. The follow-up read runs only when the update matched zero
     * rows, and only to say why: already at [status] (idempotent success), missing, or an illegal transition.
     */
    private suspend fun persistStatus(listingId: String, buyerId: String, status: ReviewRequestStatus) {
        val updated = client.from(TABLE).update({ set("status", status.name) }) {
            select(COLUMNS)
            filter {
                eq("listing_id", listingId)
                eq("buyer_id", buyerId)
                isIn("status", ReviewRequestTransitions.sourcesFor(status).map { it.name })
            }
        }.decodeList<ReviewRequestDto>()
        if (updated.isEmpty()) resolveZeroRowUpdate(listingId, buyerId, status)
    }

    private suspend fun resolveZeroRowUpdate(listingId: String, buyerId: String, status: ReviewRequestStatus) {
        val current = client.from(TABLE).select(columns = COLUMNS) {
            filter {
                eq("listing_id", listingId)
                eq("buyer_id", buyerId)
            }
        }.decodeList<ReviewRequestDto>().singleOrNull()?.let { ReviewRequestStatus.valueOf(it.status) }
            ?: throw ReviewRequestNotFoundException()
        when (ReviewRequestTransitions.decide(current, status)) {
            ReviewRequestTransition.AlreadyApplied -> Unit
            is ReviewRequestTransition.Rejected -> throw IllegalReviewRequestTransitionException(current, status)
            ReviewRequestTransition.Apply -> error("Review request status was not updated.")
        }
    }

    private suspend fun safely(action: suspend () -> Unit): Result<Unit> = runCatching {
        action()
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { error ->
            if (error is CancellationException) throw error
            Result.Error(error)
        }
    )

    private fun toDomain(dto: ReviewRequestDto) = ReviewRequest(
        MarketplaceItemId(dto.listingId),
        dto.buyerId,
        dto.sellerId,
        dto.listingTitle,
        ReviewRequestStatus.valueOf(dto.status)
    )

    private companion object {
        const val TABLE = "listing_review_requests"
        val COLUMNS = Columns.raw("listing_id,buyer_id,seller_id,listing_title,status")
    }
}
