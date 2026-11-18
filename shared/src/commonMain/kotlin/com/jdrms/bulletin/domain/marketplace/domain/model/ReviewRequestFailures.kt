package com.jdrms.bulletin.domain.marketplace.domain.model

/** No review request exists for the listing and buyer (or it is not visible to the caller). */
class ReviewRequestNotFoundException : NoSuchElementException("Review request not found.")

/** The requested status change is not in [ReviewRequestTransitions]. */
class IllegalReviewRequestTransitionException(
    val from: ReviewRequestStatus,
    val to: ReviewRequestStatus
) : IllegalStateException(ReviewRequestTransition.Rejected(from, to).message)
