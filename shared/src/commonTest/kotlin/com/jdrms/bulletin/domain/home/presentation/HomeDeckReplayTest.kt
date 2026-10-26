package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCursor
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class HomeDeckReplayTest {
    @Test
    fun replay_clears_pass_retains_save_and_does_not_fetch() = homeDeckTest { f ->
        f.signIn("first")
        f.page("passed", "saved", "available")
        advanceUntilIdle()
        f.viewModel.excludeLoadedCard(HomeItemId("passed"), HomeDeckExclusion.PASS)
        f.viewModel.excludeLoadedCard(HomeItemId("saved"), HomeDeckExclusion.SAVE)
        f.viewModel.advanceCard(0)
        f.viewModel.replayLoadedCards()
        assertEquals(listOf("passed", "available"), f.viewModel.uiState.value.feed.map { it.id.value })
        assertEquals(3, f.viewModel.uiState.value.loadedItems.size)
        assertEquals(0, f.viewModel.uiState.value.currentIndex)
        assertEquals(1, f.cursors.size)
    }

    @Test
    fun replay_does_not_retrigger_screen_prefetch_until_the_next_advance() = homeDeckTest { f ->
        f.page("first", "second", cursor = "second")
        advanceUntilIdle()
        f.viewModel.advanceCard(0)
        f.viewModel.replayLoadedCards()
        val replay = f.viewModel.uiState.value
        assertFalse(replay.prefetchEnabled)
        assertEquals(1, f.cursors.size)
        f.viewModel.advanceCard(0)
        assertEquals(true, f.viewModel.uiState.value.prefetchEnabled)
    }

    @Test
    fun refresh_retains_both_exclusions_and_uses_unfiltered_cursor() = homeDeckTest { f ->
        f.signIn("first")
        f.page("passed", "saved")
        advanceUntilIdle()
        f.viewModel.excludeLoadedCard(HomeItemId("passed"), HomeDeckExclusion.PASS)
        f.viewModel.excludeLoadedCard(HomeItemId("saved"), HomeDeckExclusion.SAVE)
        f.page("visible", "passed", "saved", cursor = "saved")
        f.viewModel.refresh()
        advanceUntilIdle()
        assertEquals(listOf("visible"), f.viewModel.uiState.value.feed.map { it.id.value })
        assertEquals(homePage(cursor = "saved").nextCursor, f.viewModel.uiState.value.nextCursor)
        assertEquals(listOf<HomeFeedCursor?>(null, null), f.cursors)
    }

    @Test
    fun account_switch_and_sign_out_discard_previous_exclusions_and_position() = homeDeckTest { f ->
        f.signIn("first")
        f.page("passed", "saved")
        advanceUntilIdle()
        f.viewModel.excludeLoadedCard(HomeItemId("passed"), HomeDeckExclusion.PASS)
        f.page("passed", "saved")
        f.signIn("second")
        advanceUntilIdle()
        assertEquals(2, f.viewModel.uiState.value.feed.size)
        f.viewModel.excludeLoadedCard(HomeItemId("saved"), HomeDeckExclusion.SAVE)
        f.viewModel.advanceCard(0)
        f.page("passed", "saved")
        f.unauthenticated()
        advanceUntilIdle()
        assertEquals(2, f.viewModel.uiState.value.feed.size)
        assertEquals(0, f.viewModel.uiState.value.currentIndex)
        f.page("passed", "saved")
        f.signIn("second")
        advanceUntilIdle()
        assertEquals(2, f.viewModel.uiState.value.feed.size)
    }

    @Test
    fun account_switch_continues_with_new_users_deck_when_old_decision_cleanup_fails() = homeDeckTest { f ->
        f.signIn("first")
        f.page("first-listing")
        advanceUntilIdle()
        f.viewModel.excludeLoadedCard(HomeItemId("first-listing"), HomeDeckExclusion.PASS)
        advanceUntilIdle()
        f.decisions.clearAllFailure = IllegalStateException("private storage failure")

        f.page("second-listing")
        f.signIn("second")
        advanceUntilIdle()

        assertEquals(listOf("second-listing"), f.viewModel.uiState.value.feed.map { it.id.value })
        assertEquals("Could not clear previous deck decisions", f.viewModel.uiState.value.errorMessage)
    }

    @Test
    fun empty_feed_and_replay_stay_empty_without_fetching() = homeDeckTest { f ->
        f.page()
        advanceUntilIdle()
        f.viewModel.replayLoadedCards()
        assertEquals(emptyList(), f.viewModel.uiState.value.feed)
        assertFalse(f.viewModel.uiState.value.isDeckLoading)
        assertFalse(f.viewModel.uiState.value.hasMore)
        assertEquals(1, f.cursors.size)
    }

    @Test
    fun all_items_excluded_ends_without_another_cursor() = homeDeckTest { f ->
        f.signIn("first")
        f.page("passed")
        advanceUntilIdle()
        f.viewModel.excludeLoadedCard(HomeItemId("passed"), HomeDeckExclusion.PASS)
        f.page("passed")
        f.viewModel.refresh()
        advanceUntilIdle()
        assertEquals(1, f.viewModel.uiState.value.loadedItems.size)
        assertEquals(emptyList(), f.viewModel.uiState.value.feed)
        assertFalse(f.viewModel.uiState.value.hasMore)
        assertFalse(f.viewModel.uiState.value.isDeckLoading)
    }

    @Test
    fun duplicate_and_stale_completion_callbacks_do_not_skip_cards() = homeDeckTest { f ->
        f.page("first", "second")
        advanceUntilIdle()
        val generation = f.viewModel.uiState.value.deckGeneration
        f.viewModel.advanceCard(0, generation)
        f.viewModel.advanceCard(0, generation)
        assertEquals(1, f.viewModel.uiState.value.currentIndex)
        f.viewModel.replayLoadedCards()
        f.viewModel.advanceCard(0, generation)
        assertEquals(0, f.viewModel.uiState.value.currentIndex)
        f.page("first", "second")
        f.viewModel.refresh()
        advanceUntilIdle()
        f.viewModel.advanceCard(0, generation)
        assertEquals(0, f.viewModel.uiState.value.currentIndex)
    }
}
