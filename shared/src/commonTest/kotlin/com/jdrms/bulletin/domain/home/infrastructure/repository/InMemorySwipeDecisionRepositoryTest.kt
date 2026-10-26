package com.jdrms.bulletin.domain.home.infrastructure.repository

import com.jdrms.bulletin.domain.home.domain.repository.SwipeDecisionRepositoryContractTest

class InMemorySwipeDecisionRepositoryTest : SwipeDecisionRepositoryContractTest() {
    override fun createRepository() = InMemorySwipeDecisionRepository()
}
