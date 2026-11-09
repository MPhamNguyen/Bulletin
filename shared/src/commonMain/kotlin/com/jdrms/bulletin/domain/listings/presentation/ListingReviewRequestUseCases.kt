package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.domain.listings.application.CancelListingReviewRequests
import com.jdrms.bulletin.domain.listings.application.CreateListingReviewRequest

data class ListingReviewRequestUseCases(
    val create: CreateListingReviewRequest?,
    val cancel: CancelListingReviewRequests?
)
