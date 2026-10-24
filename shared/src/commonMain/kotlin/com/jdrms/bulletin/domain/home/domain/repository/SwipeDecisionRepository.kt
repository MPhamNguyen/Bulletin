package com.jdrms.bulletin.domain.home.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.SwipeDecision
import com.jdrms.bulletin.domain.home.domain.model.SwipeVerdict

/** Local decision storage, isolated by authenticated user ID. Operations never call the network. */
interface SwipeDecisionRepository {
    /**
     * Atomically replaces the verdict for this user and listing. Repeating a decision is idempotent;
     * a listing belongs to exactly one verdict's exclusion set after a successful record.
     * Expected storage failures use [SwipeDecisionFailure] in [Result.Error].
     */
    suspend fun record(userId: String, decision: SwipeDecision): Result<Unit>

    /** Returns a snapshot of the user's excluded listing IDs for this verdict. */
    suspend fun getExcludedListingIds(userId: String, verdict: SwipeVerdict): Result<Set<HomeItemId>>

    /** Replay clears only Pass decisions, retaining Save decisions. */
    suspend fun clearPassDecisions(userId: String): Result<Unit>

    /** Sign-out clears all decisions for this user without affecting other users. */
    suspend fun clearAllDecisions(userId: String): Result<Unit>
}

sealed class SwipeDecisionFailure(cause: Throwable? = null) : Exception(null, cause) {
    class Storage(cause: Throwable? = null) : SwipeDecisionFailure(cause)
}
