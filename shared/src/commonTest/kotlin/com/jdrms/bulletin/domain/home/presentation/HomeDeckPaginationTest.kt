package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCursor
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeDeckPaginationTest {
    @Test
    fun exactly_three_empty_filtered_pages_stop_even_with_more_source_pages() = homeDeckTest { f ->
        f.signIn("first")
        f.page("excluded")
        advanceUntilIdle()
        f.viewModel.excludeLoadedCard(HomeItemId("excluded"), HomeDeckExclusion.PASS)
        repeat(3) { f.page("excluded", cursor = "cursor-$it") }
        f.page("beyond-cap", cursor = "cursor-3")
        f.viewModel.refresh()
        advanceUntilIdle()
        assertEquals(4, f.cursors.size)
        assertEquals(1, f.responses.size)
        assertEquals(homePage(cursor = "cursor-2").nextCursor, f.viewModel.uiState.value.nextCursor)
        assertFalse(f.viewModel.uiState.value.hasMore)
        assertFalse(f.viewModel.uiState.value.isDeckLoading)
        f.viewModel.loadNextPage()
        advanceUntilIdle()
        assertEquals(4, f.cursors.size)
    }

    @Test
    fun exactly_at_cap_stops_with_no_extra_request() = homeDeckTest { f ->
        repeat(3) { f.page(cursor = "empty-$it") }
        advanceUntilIdle()
        assertEquals(3, f.cursors.size)
        assertEquals(emptyList(), f.viewModel.uiState.value.feed)
        assertFalse(f.viewModel.uiState.value.hasMore)
        assertFalse(f.viewModel.uiState.value.isDeckLoading)
    }

    @Test
    fun visible_third_page_is_shown_and_resets_empty_page_budget() = homeDeckTest { f ->
        f.signIn("first")
        f.page("excluded")
        advanceUntilIdle()
        f.viewModel.excludeLoadedCard(HomeItemId("excluded"), HomeDeckExclusion.SAVE)
        f.page("excluded", cursor = "a")
        f.page("excluded", cursor = "b")
        f.page("visible", cursor = "c")
        f.viewModel.refresh()
        advanceUntilIdle()
        assertEquals(listOf("visible"), f.viewModel.uiState.value.feed.map { it.id.value })
        assertTrue(f.viewModel.uiState.value.hasMore)
        repeat(3) { f.page("excluded", cursor = "next-$it") }
        f.viewModel.loadNextPage()
        advanceUntilIdle()
        assertEquals(7, f.cursors.size)
        assertFalse(f.viewModel.uiState.value.hasMore)
    }

    @Test
    fun empty_source_pages_prefetch_with_their_own_cursors_until_a_card_arrives() = homeDeckTest { f ->
        f.page(cursor = "a")
        f.page(cursor = "b")
        f.page("visible")
        advanceUntilIdle()
        assertEquals(listOf(null, homePage(cursor = "a").nextCursor, homePage(cursor = "b").nextCursor), f.cursors)
        assertEquals(listOf("visible"), f.viewModel.uiState.value.feed.map { it.id.value })
    }

    @Test
    fun repeated_refresh_cancels_previous_initial_load_and_resets_position() = homeDeckTest { f ->
        f.responses.add { try { awaitCancellation() } finally { f.cancellations++ } }
        runCurrent()
        f.responses.add { try { awaitCancellation() } finally { f.cancellations++ } }
        f.viewModel.refresh()
        runCurrent()
        f.page("latest")
        f.viewModel.refresh()
        advanceUntilIdle()
        assertEquals(2, f.cancellations)
        assertEquals(listOf<HomeFeedCursor?>(null, null, null), f.cursors)
        assertEquals(listOf("latest"), f.viewModel.uiState.value.feed.map { it.id.value })
        assertEquals(0, f.viewModel.uiState.value.currentIndex)
    }

    @Test
    fun refresh_cancels_append_and_clears_loaded_items_and_cursor_immediately() = homeDeckTest { f ->
        f.page("old", cursor = "old")
        advanceUntilIdle()
        f.responses.add { try { awaitCancellation() } finally { f.cancellations++ } }
        f.viewModel.loadNextPage()
        runCurrent()
        f.page("fresh")
        f.viewModel.refresh()
        assertEquals(emptyList(), f.viewModel.uiState.value.loadedItems)
        assertEquals(null, f.viewModel.uiState.value.nextCursor)
        assertTrue(f.viewModel.uiState.value.isLoading)
        advanceUntilIdle()
        assertEquals(1, f.cancellations)
        assertEquals(listOf("fresh"), f.viewModel.uiState.value.feed.map { it.id.value })
        assertEquals(listOf(null, homePage(cursor = "old").nextCursor, null), f.cursors)
    }

    @Test
    fun stale_non_cooperative_append_result_cannot_overwrite_refresh() = homeDeckTest { f ->
        val stale = CompletableDeferred<Unit>()
        f.page("old", cursor = "old")
        advanceUntilIdle()
        f.responses.add {
            withContext(NonCancellable) {
                stale.await()
                homePage("stale")
            }
        }
        f.viewModel.loadNextPage()
        runCurrent()
        f.page("fresh")
        f.viewModel.refresh()
        runCurrent()
        stale.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("fresh"), f.viewModel.uiState.value.feed.map { it.id.value })
    }

    @Test
    fun exhausted_deck_loads_next_page_then_continues_from_owned_position() = homeDeckTest { f ->
        val next = CompletableDeferred<Unit>()
        f.page("first", cursor = "first")
        advanceUntilIdle()
        f.responses.add {
            next.await()
            homePage("second", cursor = "second")
        }
        f.viewModel.loadNextPage()
        f.viewModel.advanceCard(0)
        runCurrent()
        assertTrue(f.viewModel.uiState.value.isDeckLoading)
        assertEquals(null, f.viewModel.uiState.value.feed.getOrNull(f.viewModel.uiState.value.currentIndex))
        next.complete(Unit)
        advanceUntilIdle()
        assertEquals("second", f.viewModel.uiState.value.feed[f.viewModel.uiState.value.currentIndex].id.value)
        f.page("third")
        f.viewModel.loadNextPage()
        advanceUntilIdle()
        f.viewModel.advanceCard(1)
        assertEquals("third", f.viewModel.uiState.value.feed[f.viewModel.uiState.value.currentIndex].id.value)
    }

    @Test
    fun filtered_append_failure_preserves_items_cursor_and_can_retry() = homeDeckTest { f ->
        f.signIn("first")
        f.page("first", "excluded", cursor = "excluded")
        advanceUntilIdle()
        f.viewModel.excludeLoadedCard(HomeItemId("excluded"), HomeDeckExclusion.PASS)
        f.responses.add { error("Temporary failure") }
        f.viewModel.loadNextPage()
        advanceUntilIdle()
        assertEquals(listOf("first"), f.viewModel.uiState.value.feed.map { it.id.value })
        assertEquals(2, f.viewModel.uiState.value.loadedItems.size)
        assertEquals(homePage(cursor = "excluded").nextCursor, f.viewModel.uiState.value.nextCursor)
        assertTrue(f.viewModel.uiState.value.hasMore)
        assertEquals("Failed to load listings", f.viewModel.uiState.value.errorMessage)
        f.page("excluded", "second")
        f.viewModel.loadNextPage()
        advanceUntilIdle()
        assertEquals(listOf("first", "second"), f.viewModel.uiState.value.feed.map { it.id.value })
        assertEquals(f.cursors[1], f.cursors[2])
        assertEquals(null, f.viewModel.uiState.value.errorMessage)
    }
}
