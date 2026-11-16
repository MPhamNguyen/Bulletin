package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest

internal fun selectReviewRequestToPresent(
    currentlyPresented: ReviewRequest?,
    actionableRequests: List<ReviewRequest>
): ReviewRequest? {
    val eligible = actionableRequests.filter { it.isActionableByBuyer }
    val currentStillActionable = currentlyPresented?.let { current ->
        eligible.firstOrNull { it.listingId == current.listingId && it.buyerId == current.buyerId }
    }
    return currentStillActionable ?: eligible.firstOrNull()
}
