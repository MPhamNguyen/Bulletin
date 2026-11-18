package com.jdrms.bulletin.domain.marketplace.domain.model

enum class ReviewRequestStatus { PENDING, ACCEPTED, DECLINED, SUBMITTED, CANCELLED }

data class ReviewRequest(
    val listingId: MarketplaceItemId,
    val buyerId: String,
    val sellerId: String,
    val listingTitle: String,
    val status: ReviewRequestStatus = ReviewRequestStatus.PENDING
) {
    init {
        require(buyerId.isNotBlank()) { "Review request buyer cannot be blank." }
        require(sellerId.isNotBlank()) { "Review request seller cannot be blank." }
        require(buyerId != sellerId) { "A seller cannot request a review from themselves." }
        require(listingTitle.isNotBlank()) { "Review request listing title cannot be blank." }
    }

    fun accept(): ReviewRequest = transition(ReviewRequestStatus.ACCEPTED)
    fun decline(): ReviewRequest = transition(ReviewRequestStatus.DECLINED)
    fun submitted(): ReviewRequest = transition(ReviewRequestStatus.SUBMITTED)
    fun cancel(): ReviewRequest = transition(ReviewRequestStatus.CANCELLED)

    /** True while the buyer can still accept, decline or submit. Terminal requests are not actionable. */
    val isActionableByBuyer: Boolean get() = ReviewRequestTransitions.isActionableByBuyer(status)

    fun canTransitionTo(next: ReviewRequestStatus): Boolean = ReviewRequestTransitions.isLegal(status, next)

    private fun transition(next: ReviewRequestStatus): ReviewRequest =
        when (val decision = ReviewRequestTransitions.decide(status, next)) {
            ReviewRequestTransition.Apply -> copy(status = next)
            ReviewRequestTransition.AlreadyApplied -> this
            is ReviewRequestTransition.Rejected -> throw IllegalArgumentException(decision.message)
        }
}
