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

    private fun transition(next: ReviewRequestStatus): ReviewRequest {
        require(status == ReviewRequestStatus.PENDING || status == ReviewRequestStatus.ACCEPTED) {
            "A declined or submitted review request cannot be changed."
        }
        require(next != ReviewRequestStatus.DECLINED || status == ReviewRequestStatus.PENDING) {
            "An accepted review request cannot be declined."
        }
        require(
            next != ReviewRequestStatus.CANCELLED ||
                status == ReviewRequestStatus.PENDING || status == ReviewRequestStatus.ACCEPTED
        ) { "A completed review request cannot be cancelled." }
        return copy(status = next)
    }
}
