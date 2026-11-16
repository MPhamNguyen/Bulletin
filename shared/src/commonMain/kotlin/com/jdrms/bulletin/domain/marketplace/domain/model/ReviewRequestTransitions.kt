package com.jdrms.bulletin.domain.marketplace.domain.model

/**
 * The single source of truth for review-request status changes.
 *
 * PENDING -> SUBMITTED is intentional: a buyer may submit a review without accepting the request first.
 * DECLINED, SUBMITTED and CANCELLED are terminal.
 *
 * Repeating a buyer-settable status that is already in place (ACCEPTED, DECLINED or SUBMITTED) is not a
 * transition; it is an idempotent no-op that reports success ([ReviewRequestTransition.AlreadyApplied]).
 * Every other change that is not in the table is rejected.
 *
 * The database trigger in `supabase/migrations/20261116000000_enforce_review_request_transitions.sql`
 * mirrors this table.
 */
object ReviewRequestTransitions {
    private val legal: Map<ReviewRequestStatus, Set<ReviewRequestStatus>> = mapOf(
        ReviewRequestStatus.PENDING to setOf(
            ReviewRequestStatus.ACCEPTED,
            ReviewRequestStatus.DECLINED,
            ReviewRequestStatus.SUBMITTED,
            ReviewRequestStatus.CANCELLED
        ),
        ReviewRequestStatus.ACCEPTED to setOf(ReviewRequestStatus.SUBMITTED, ReviewRequestStatus.CANCELLED),
        ReviewRequestStatus.DECLINED to emptySet(),
        ReviewRequestStatus.SUBMITTED to emptySet(),
        ReviewRequestStatus.CANCELLED to emptySet()
    )

    /** Statuses a buyer may request. Cancellation belongs to the seller; PENDING is only an initial state. */
    val buyerSettable: Set<ReviewRequestStatus> = setOf(
        ReviewRequestStatus.ACCEPTED,
        ReviewRequestStatus.DECLINED,
        ReviewRequestStatus.SUBMITTED
    )

    fun isLegal(from: ReviewRequestStatus, to: ReviewRequestStatus): Boolean = to in legal.getValue(from)

    fun isTerminal(status: ReviewRequestStatus): Boolean = legal.getValue(status).isEmpty()

    /** A request the buyer can still act on (accept, decline, submit). */
    fun isActionableByBuyer(status: ReviewRequestStatus): Boolean = !isTerminal(status)

    /** Every status from which [to] is legal, in declaration order. */
    fun sourcesFor(to: ReviewRequestStatus): List<ReviewRequestStatus> =
        ReviewRequestStatus.entries.filter { isLegal(it, to) }

    fun decide(from: ReviewRequestStatus, to: ReviewRequestStatus): ReviewRequestTransition = when {
        isLegal(from, to) -> ReviewRequestTransition.Apply
        from == to && to in buyerSettable -> ReviewRequestTransition.AlreadyApplied
        else -> ReviewRequestTransition.Rejected(from, to)
    }
}

sealed interface ReviewRequestTransition {
    /** The change is in the table and should be written. */
    data object Apply : ReviewRequestTransition

    /** The request already has the requested buyer status; nothing to write, report success. */
    data object AlreadyApplied : ReviewRequestTransition

    data class Rejected(val from: ReviewRequestStatus, val to: ReviewRequestStatus) : ReviewRequestTransition {
        val message: String get() = "A $from review request cannot change to $to."
    }
}
