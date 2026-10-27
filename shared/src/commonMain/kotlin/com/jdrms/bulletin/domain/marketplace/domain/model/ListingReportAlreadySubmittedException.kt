package com.jdrms.bulletin.domain.marketplace.domain.model

class ListingReportAlreadySubmittedException : IllegalStateException(
    "You have already submitted an open report for this listing."
)
