package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus
import com.jdrms.bulletin.domain.marketplace.domain.repository.ReviewRequestRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.ReviewRequestDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException

class SupabaseReviewRequestRepository(private val client: SupabaseClient) : ReviewRequestRepository {
    override suspend fun createIfMissing(request: ReviewRequest): Result<Unit> = safely {
        require(
            client.auth.currentUserOrNull()?.id == request.sellerId
        ) { "Only the seller can create a review request." }
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
        if (client.auth.currentUserOrNull()?.id != buyerId) return emptyList()
        return client.from(TABLE).select(columns = Columns.raw("listing_id,buyer_id,seller_id,listing_title,status")) {
            filter { eq("buyer_id", buyerId) }
        }.decodeList<ReviewRequestDto>().map(::toDomain).filter {
            it.status == ReviewRequestStatus.PENDING || it.status == ReviewRequestStatus.ACCEPTED
        }
    }

    override suspend fun updateStatus(
        listingId: String,
        buyerId: String,
        status: ReviewRequestStatus
    ): Result<Unit> = safely {
        require(client.auth.currentUserOrNull()?.id == buyerId) { "Only the buyer can update a review request." }
        require(
            status != ReviewRequestStatus.PENDING && status != ReviewRequestStatus.CANCELLED
        ) { "Buyers cannot set this review request status." }
        persistStatus(listingId, buyerId, status)
    }

    override suspend fun cancelForRestoredListing(listingId: String, sellerId: String): Result<Unit> = safely {
        require(client.auth.currentUserOrNull()?.id == sellerId) { "Only the seller can cancel review requests." }
        val id = listingId.removePrefix("listing:")
        cancelStatusFrom(id, sellerId, ReviewRequestStatus.PENDING)
        cancelStatusFrom(id, sellerId, ReviewRequestStatus.ACCEPTED)
        val activeStatuses = client.from(TABLE)
            .select(columns = Columns.raw("listing_id,buyer_id,seller_id,listing_title,status")) {
                filter {
                    eq("listing_id", id)
                    eq("seller_id", sellerId)
                }
            }
            .decodeList<ReviewRequestDto>()
            .map(ReviewRequestDto::status)
            .filter { it == ReviewRequestStatus.PENDING.name || it == ReviewRequestStatus.ACCEPTED.name }
        check(activeStatuses.isEmpty()) { "Outstanding review requests could not be cancelled." }
    }

    private suspend fun persistStatus(listingId: String, buyerId: String, status: ReviewRequestStatus) {
        val id = listingId.removePrefix("listing:")
        val source = if (status == ReviewRequestStatus.SUBMITTED) {
            ReviewRequestStatus.ACCEPTED
        } else {
            ReviewRequestStatus.PENDING
        }
        updateStatusFrom(id, buyerId, source, status)
        var current = fetchStatus(id, buyerId)
        if (status == ReviewRequestStatus.SUBMITTED && current == ReviewRequestStatus.PENDING.name) {
            updateStatusFrom(id, buyerId, ReviewRequestStatus.PENDING, status)
            current = fetchStatus(id, buyerId)
        }
        check(current == status.name) { "Review request status was not updated." }
    }

    private suspend fun updateStatusFrom(
        listingId: String,
        buyerId: String,
        from: ReviewRequestStatus,
        to: ReviewRequestStatus
    ) {
        client.from(TABLE).update({ set("status", to.name) }) {
            filter {
                eq("listing_id", listingId)
                eq("buyer_id", buyerId)
                eq("status", from.name)
            }
        }
    }

    private suspend fun cancelStatusFrom(
        listingId: String,
        sellerId: String,
        status: ReviewRequestStatus
    ) {
        client.from(TABLE).update({ set("status", ReviewRequestStatus.CANCELLED.name) }) {
            filter {
                eq("listing_id", listingId)
                eq("seller_id", sellerId)
                eq("status", status.name)
            }
        }
    }

    private suspend fun fetchStatus(listingId: String, buyerId: String): String? =
        client.from(TABLE).select(columns = Columns.raw("listing_id,buyer_id,seller_id,listing_title,status")) {
            filter {
                eq("listing_id", listingId)
                eq("buyer_id", buyerId)
            }
        }.decodeList<ReviewRequestDto>().singleOrNull()?.status

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

    private companion object { const val TABLE = "listing_review_requests" }
}
