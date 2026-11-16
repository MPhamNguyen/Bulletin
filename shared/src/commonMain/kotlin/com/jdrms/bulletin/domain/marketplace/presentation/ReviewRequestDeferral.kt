package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest

/** Deferral is presentation state; which requests are worth prompting for is a domain rule. */
internal fun List<ReviewRequest>.excludingDeferredReviewRequests(deferredListingIds: Set<String>): List<ReviewRequest> =
    filter { it.isActionableByBuyer && it.listingId.value !in deferredListingIds }
