package com.jdrms.bulletin.domain.home.domain.repository

import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.SwipeDecision
import com.jdrms.bulletin.domain.home.domain.model.SwipeVerdict
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Reuse against the Feature 4 implementation; currently exercised only against the test fake. */
abstract class SwipeDecisionRepositoryContractTest {
    protected abstract fun createRepository(): SwipeDecisionRepository

    @Test
    fun usersHaveIndependentVerdictsForTheSameListing() = runTest {
        val repository = createRepository()
        val listing = HomeItemId("listing")
        repository.record("first", SwipeDecision(listing, SwipeVerdict.PASS)).getOrThrow()
        repository.record("second", SwipeDecision(listing, SwipeVerdict.SAVE)).getOrThrow()

        assertEquals(setOf(listing), repository.getExcludedListingIds("first", SwipeVerdict.PASS).getOrThrow())
        assertEquals(emptySet(), repository.getExcludedListingIds("first", SwipeVerdict.SAVE).getOrThrow())
        assertEquals(setOf(listing), repository.getExcludedListingIds("second", SwipeVerdict.SAVE).getOrThrow())
        assertEquals(emptySet(), repository.getExcludedListingIds("second", SwipeVerdict.PASS).getOrThrow())
    }

    @Test
    fun replayClearsOnlyThisUsersPassDecisions() = runTest {
        val repository = createRepository()
        val passed = HomeItemId("passed")
        val saved = HomeItemId("saved")
        repository.record("first", SwipeDecision(passed, SwipeVerdict.PASS)).getOrThrow()
        repository.record("first", SwipeDecision(saved, SwipeVerdict.SAVE)).getOrThrow()
        repository.record("second", SwipeDecision(passed, SwipeVerdict.PASS)).getOrThrow()

        repository.clearPassDecisions("first").getOrThrow()
        repository.clearPassDecisions("first").getOrThrow()

        assertEquals(emptySet(), repository.getExcludedListingIds("first", SwipeVerdict.PASS).getOrThrow())
        assertEquals(setOf(saved), repository.getExcludedListingIds("first", SwipeVerdict.SAVE).getOrThrow())
        assertEquals(setOf(passed), repository.getExcludedListingIds("second", SwipeVerdict.PASS).getOrThrow())
    }

    @Test
    fun signOutClearsOnlyThisUsersDecisions() = runTest {
        val repository = createRepository()
        val listing = HomeItemId("listing")
        val saved = HomeItemId("saved")
        repository.record("first", SwipeDecision(listing, SwipeVerdict.PASS)).getOrThrow()
        repository.record("first", SwipeDecision(saved, SwipeVerdict.SAVE)).getOrThrow()
        repository.record("second", SwipeDecision(listing, SwipeVerdict.SAVE)).getOrThrow()

        repository.clearAllDecisions("first").getOrThrow()
        repository.clearAllDecisions("first").getOrThrow()

        assertEquals(emptySet(), repository.getExcludedListingIds("first", SwipeVerdict.PASS).getOrThrow())
        assertEquals(emptySet(), repository.getExcludedListingIds("first", SwipeVerdict.SAVE).getOrThrow())
        assertEquals(setOf(listing), repository.getExcludedListingIds("second", SwipeVerdict.SAVE).getOrThrow())
    }

    @Test
    fun exclusionReadsAreSnapshots() = runTest {
        val repository = createRepository()
        val first = HomeItemId("first")
        repository.record("viewer", SwipeDecision(first, SwipeVerdict.PASS)).getOrThrow()
        val snapshot = repository.getExcludedListingIds("viewer", SwipeVerdict.PASS).getOrThrow()

        repository.record("viewer", SwipeDecision(HomeItemId("second"), SwipeVerdict.PASS)).getOrThrow()
        repository.clearAllDecisions("viewer").getOrThrow()

        assertEquals(setOf(first), snapshot)
    }
}
