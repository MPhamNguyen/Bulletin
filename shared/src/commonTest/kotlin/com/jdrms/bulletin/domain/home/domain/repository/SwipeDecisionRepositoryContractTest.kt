package com.jdrms.bulletin.domain.home.domain.repository

import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.SwipeDecision
import com.jdrms.bulletin.domain.home.domain.model.SwipeVerdict
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Observable storage contract shared by the fake and process-local implementation. */
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
    fun recordingReplacesThePreviousVerdictIdempotently() = runTest {
        val repository = createRepository()
        val listing = HomeItemId("listing")
        repository.record("viewer", SwipeDecision(listing, SwipeVerdict.PASS)).getOrThrow()
        repeat(2) { repository.record("viewer", SwipeDecision(listing, SwipeVerdict.SAVE)).getOrThrow() }
        assertEquals(emptySet(), repository.getExcludedListingIds("viewer", SwipeVerdict.PASS).getOrThrow())
        assertEquals(setOf(listing), repository.getExcludedListingIds("viewer", SwipeVerdict.SAVE).getOrThrow())
        repository.record("viewer", SwipeDecision(listing, SwipeVerdict.PASS)).getOrThrow()
        assertEquals(emptySet(), repository.getExcludedListingIds("viewer", SwipeVerdict.SAVE).getOrThrow())
        assertEquals(setOf(listing), repository.getExcludedListingIds("viewer", SwipeVerdict.PASS).getOrThrow())
    }

    @Test
    fun removeSaveIsIsolatedAndDoesNotRemoveALaterPass() = runTest {
        val repository = createRepository()
        val listing = HomeItemId("listing")
        repository.removeSave("first", listing).getOrThrow()
        repository.record("first", SwipeDecision(listing, SwipeVerdict.SAVE)).getOrThrow()
        repository.record("second", SwipeDecision(listing, SwipeVerdict.SAVE)).getOrThrow()
        repository.removeSave("first", listing).getOrThrow()
        repository.removeSave("first", listing).getOrThrow()
        assertEquals(emptySet(), repository.getExcludedListingIds("first", SwipeVerdict.SAVE).getOrThrow())
        assertEquals(setOf(listing), repository.getExcludedListingIds("second", SwipeVerdict.SAVE).getOrThrow())
        repository.record("first", SwipeDecision(listing, SwipeVerdict.SAVE)).getOrThrow()
        repository.record("first", SwipeDecision(listing, SwipeVerdict.PASS)).getOrThrow()
        repository.removeSave("first", listing).getOrThrow()
        assertEquals(setOf(listing), repository.getExcludedListingIds("first", SwipeVerdict.PASS).getOrThrow())
    }

    @Test
    fun concurrentRecordsFinishWithOneVerdictPerListing() = runTest {
        val repository = createRepository()
        coroutineScope {
            repeat(200) { index ->
                launch {
                    val listing = HomeItemId("listing-${index % 20}")
                    repository.record("viewer", SwipeDecision(listing, SwipeVerdict.SAVE)).getOrThrow()
                    repository.record("viewer", SwipeDecision(listing, SwipeVerdict.PASS)).getOrThrow()
                }
            }
        }
        assertEquals(emptySet(), repository.getExcludedListingIds("viewer", SwipeVerdict.SAVE).getOrThrow())
        assertEquals(
            (0 until 20).map { HomeItemId("listing-$it") }.toSet(),
            repository.getExcludedListingIds("viewer", SwipeVerdict.PASS).getOrThrow()
        )
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
