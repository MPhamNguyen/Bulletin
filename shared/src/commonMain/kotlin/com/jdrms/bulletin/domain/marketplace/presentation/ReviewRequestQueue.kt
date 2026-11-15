package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest

internal fun advanceReviewRequestQueue(
    resolved: ReviewRequest,
    queuedRequests: List<ReviewRequest>
): List<ReviewRequest> = queuedRequests.filterNot { it.listingId == resolved.listingId }

internal fun mergeReviewRequestQueue(
    queuedRequests: List<ReviewRequest>,
    latestRequests: List<ReviewRequest>
): List<ReviewRequest> {
    val latestByKey = latestRequests.associateBy { it.listingId.value to it.buyerId }
    val retainedKeys = mutableSetOf<Pair<String, String>>()
    val retained = queuedRequests.mapNotNull { queued ->
        val key = queued.listingId.value to queued.buyerId
        latestByKey[key]?.also { retainedKeys += key }
    }
    return retained + latestRequests.filterNot { (it.listingId.value to it.buyerId) in retainedKeys }
}
