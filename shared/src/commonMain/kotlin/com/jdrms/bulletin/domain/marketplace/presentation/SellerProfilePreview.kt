package com.jdrms.bulletin.domain.marketplace.presentation

/** Identity already displayed at the entry point; remaining profile details are still unknown. */
data class SellerProfilePreview(
    val sellerId: String,
    val name: String?,
    val avatarUrl: String? = null
)
