package com.jdrms.bulletin.domain.home.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.SwipeDecision
import com.jdrms.bulletin.domain.home.domain.model.SwipeVerdict
import com.jdrms.bulletin.domain.home.domain.repository.SwipeDecisionFailure
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class RecordSwipeDecisionTest {
    private val repository = FakeSwipeDecisionRepository()
    private val record = RecordSwipeDecision(repository)
    private val listingId = HomeItemId("listing")

    @Test
    fun firstRecordStoresListingAndVerdictForViewer() = runTest {
        assertIs<Result.Success<Unit>>(record("viewer", SwipeDecision(listingId, SwipeVerdict.PASS)))

        assertEquals(setOf(listingId), repository.getExcludedListingIds("viewer", SwipeVerdict.PASS).getOrThrow())
        assertEquals(emptySet(), repository.getExcludedListingIds("viewer", SwipeVerdict.SAVE).getOrThrow())
        assertEquals(1, repository.recordCalls)
    }

    @Test
    fun duplicateRecordIsIdempotent() = runTest {
        val decision = SwipeDecision(listingId, SwipeVerdict.SAVE)
        assertIs<Result.Success<Unit>>(record("viewer", decision))
        assertIs<Result.Success<Unit>>(record("viewer", decision))

        assertEquals(setOf(listingId), repository.getExcludedListingIds("viewer", SwipeVerdict.SAVE).getOrThrow())
        assertEquals(emptySet(), repository.getExcludedListingIds("viewer", SwipeVerdict.PASS).getOrThrow())
    }

    @Test
    fun passThenSaveKeepsOnlySave() = runTest {
        assertLatestVerdict(SwipeVerdict.PASS, SwipeVerdict.SAVE)
    }

    @Test
    fun saveThenPassKeepsOnlyPass() = runTest {
        assertLatestVerdict(SwipeVerdict.SAVE, SwipeVerdict.PASS)
    }

    @Test
    fun missingViewerFailsWithoutCallingRepository() = runTest {
        listOf(null, "", " \t").forEach { viewer ->
            val result = record(viewer, SwipeDecision(listingId, SwipeVerdict.SAVE))
            assertIs<RecordSwipeDecisionFailure.NotSignedIn>(assertIs<Result.Error>(result).exception)
        }
        assertEquals(0, repository.recordCalls)
    }

    @Test
    fun repositoryFailureBecomesTypedStorageFailure() = runTest {
        val failure = SwipeDecisionFailure.Storage()
        repository.failure = failure

        val result = record("viewer", SwipeDecision(listingId, SwipeVerdict.SAVE))

        val error = assertIs<RecordSwipeDecisionFailure.Storage>(assertIs<Result.Error>(result).exception)
        assertSame(failure, error.cause)
        assertEquals(emptySet(), repository.getExcludedListingIds("viewer", SwipeVerdict.SAVE).getOrThrow())
    }

    private suspend fun assertLatestVerdict(first: SwipeVerdict, latest: SwipeVerdict) {
        assertIs<Result.Success<Unit>>(record("viewer", SwipeDecision(listingId, first)))
        assertIs<Result.Success<Unit>>(record("viewer", SwipeDecision(listingId, latest)))

        assertEquals(emptySet(), repository.getExcludedListingIds("viewer", first).getOrThrow())
        assertEquals(setOf(listingId), repository.getExcludedListingIds("viewer", latest).getOrThrow())
    }
}
