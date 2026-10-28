package com.jdrms.bulletin.domain.home.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.SwipeDecision
import com.jdrms.bulletin.domain.home.domain.model.SwipeVerdict
import com.jdrms.bulletin.domain.home.domain.repository.SwipeDecisionRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Process-local decisions shared by the Home flows in one application container. */
class InMemorySwipeDecisionRepository : SwipeDecisionRepository {
    private val mutex = Mutex()
    private val decisions = mutableMapOf<String, MutableMap<HomeItemId, SwipeVerdict>>()

    override suspend fun record(userId: String, decision: SwipeDecision): Result<Unit> = mutex.withLock {
        decisions.getOrPut(userId) { mutableMapOf() }[decision.listingId] = decision.verdict
        Result.Success(Unit)
    }

    override suspend fun getExcludedListingIds(userId: String, verdict: SwipeVerdict): Result<Set<HomeItemId>> =
        mutex.withLock {
            Result.Success(decisions[userId].orEmpty().filterValues { it == verdict }.keys.toSet())
        }

    override suspend fun clearPassDecisions(userId: String): Result<Unit> = mutex.withLock {
        decisions[userId]?.entries?.removeAll { it.value == SwipeVerdict.PASS }
        Result.Success(Unit)
    }

    override suspend fun removeSave(userId: String, listingId: HomeItemId): Result<Unit> = mutex.withLock {
        if (decisions[userId]?.get(listingId) == SwipeVerdict.SAVE) decisions[userId]?.remove(listingId)
        Result.Success(Unit)
    }

    override suspend fun clearAllDecisions(userId: String): Result<Unit> = mutex.withLock {
        decisions.remove(userId)
        Result.Success(Unit)
    }
}
