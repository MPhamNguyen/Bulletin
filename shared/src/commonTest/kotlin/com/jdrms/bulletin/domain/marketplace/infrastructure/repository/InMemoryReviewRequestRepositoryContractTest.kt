package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.ReviewRequestRepositoryContract
import com.jdrms.bulletin.domain.marketplace.domain.repository.ReviewRequestRepository

class InMemoryReviewRequestRepositoryContractTest : ReviewRequestRepositoryContract() {
    override fun newRepository(): ReviewRequestRepository = InMemoryReviewRequestRepository()
}
