package com.jdrms.bulletin.domain.marketplace.presentation

internal data class SellerInfoCardState(
    val name: String,
    val school: String,
    val avatarUrl: String?,
    val reputationScore: Double?,
    val major: String,
    val reviewCount: Int,
    val isVerified: Boolean
)
