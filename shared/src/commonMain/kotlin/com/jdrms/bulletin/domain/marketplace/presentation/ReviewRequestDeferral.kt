package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest

internal fun List<ReviewRequest>.excludingDeferredReviewRequests(deferredListingIds: Set<String>): List<ReviewRequest> =
    filterNot { it.listingId.value in deferredListingIds }
