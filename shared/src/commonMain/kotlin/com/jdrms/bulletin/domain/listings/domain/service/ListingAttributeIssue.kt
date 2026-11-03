package com.jdrms.bulletin.domain.listings.domain.service

/** A validation problem for one attribute [key], or for the attribute set as a whole when [key] is null. */
data class ListingAttributeIssue(val key: String?, val message: String)
