package com.jdrms.bulletin.domain.home.application

import com.jdrms.bulletin.domain.home.domain.repository.SwipeDecisionRepository
import com.jdrms.bulletin.domain.home.domain.repository.SwipeDecisionRepositoryContractTest

class FakeSwipeDecisionRepositoryContractTest : SwipeDecisionRepositoryContractTest() {
    override fun createRepository(): SwipeDecisionRepository = FakeSwipeDecisionRepository()
}
