package com.jdrms.bulletin.domain.home.domain.model

enum class SwipeVerdict {
    SAVE,
    PASS
}

data class SwipeDecision(val listingId: HomeItemId, val verdict: SwipeVerdict)
