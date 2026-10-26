package com.jdrms.bulletin.domain.home.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.SwipeDecision
import com.jdrms.bulletin.domain.home.domain.model.SwipeVerdict
import com.jdrms.bulletin.domain.home.domain.repository.SwipeDecisionRepository

internal class FakeSwipeDecisionRepository : SwipeDecisionRepository {
    private val decisions = mutableMapOf<String, MutableMap<HomeItemId, SwipeVerdict>>()
    var failure: Throwable? = null
    var clearAllFailure: Throwable? = null
    var clearPassFailure: Throwable? = null
    var recordCalls = 0
        private set

    override suspend fun record(userId: String, decision: SwipeDecision): Result<Unit> {
        recordCalls++
        failure?.let { return Result.Error(it) }
        decisions.getOrPut(userId) { mutableMapOf() }[decision.listingId] = decision.verdict
        return Result.Success(Unit)
    }

    override suspend fun getExcludedListingIds(userId: String, verdict: SwipeVerdict): Result<Set<HomeItemId>> {
        return Result.Success(decisions[userId].orEmpty().filterValues { it == verdict }.keys.toSet())
    }

    override suspend fun clearPassDecisions(userId: String): Result<Unit> {
        clearPassFailure?.let { return Result.Error(it) }
        decisions[userId]?.entries?.removeAll { it.value == SwipeVerdict.PASS }
        return Result.Success(Unit)
    }

    override suspend fun removeSave(userId: String, listingId: HomeItemId): Result<Unit> {
        if (decisions[userId]?.get(listingId) == SwipeVerdict.SAVE) decisions[userId]?.remove(listingId)
        return Result.Success(Unit)
    }

    override suspend fun clearAllDecisions(userId: String): Result<Unit> {
        clearAllFailure?.let { return Result.Error(it) }
        decisions.remove(userId)
        return Result.Success(Unit)
    }
}
