package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest

internal fun selectReviewRequestToPresent(
    currentlyPresented: ReviewRequest?,
    actionableRequests: List<ReviewRequest>
): ReviewRequest? {
    val currentStillActionable = currentlyPresented?.let { current ->
        actionableRequests.firstOrNull { it.listingId == current.listingId && it.buyerId == current.buyerId }
    }
    return currentStillActionable ?: actionableRequests.firstOrNull()
}
