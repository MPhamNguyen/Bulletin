package com.jdrms.bulletin.domain.marketplace.domain.model

class ListingReportSelfReportException(cause: Throwable? = null) :
    IllegalStateException("You cannot report your own listing.", cause)
