package com.jdrms.bulletin.domain.profile.domain.model

data class StudentReviewPage(
    val reviews: List<StudentReview>,
    val nextOffset: Int?
)
