package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedPage
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.SwipeDecision
import com.jdrms.bulletin.domain.home.domain.model.SwipeVerdict
import com.jdrms.bulletin.domain.home.domain.repository.SwipeDecisionRepository

internal enum class HomeDeckExclusion { PASS, SAVE }

/** Deck filtering and pagination policy backed by session decision storage. */
internal class HomeDeckPolicy(private val decisions: SwipeDecisionRepository) {
    private var userId: String? = null
    private var emptyPages = 0

    suspend fun changeAccount(id: String?) {
        userId?.let { decisions.clearAllDecisions(it).getOrThrow() }
        userId = id
        emptyPages = 0
    }

    fun resetPagination() {
        emptyPages = 0
    }

    suspend fun exclude(id: HomeItemId, exclusion: HomeDeckExclusion) {
        val owner = userId ?: return
        decisions.record(owner, SwipeDecision(id, SwipeVerdict.valueOf(exclusion.name))).getOrThrow()
    }

    suspend fun excludedIds(): Set<HomeItemId> {
        val owner = userId ?: return emptySet()
        return decisions.getExcludedListingIds(owner, SwipeVerdict.PASS).getOrThrow() +
            decisions.getExcludedListingIds(owner, SwipeVerdict.SAVE).getOrThrow()
    }

    fun eligible(items: List<HomeFeedItem>, excluded: Set<HomeItemId>): List<HomeFeedItem> =
        items.filterNot { it.id in excluded }

    suspend fun clearPassDecisions() {
        userId?.let { decisions.clearPassDecisions(it).getOrThrow() }
    }

    fun replay(state: HomeUiState, excluded: Set<HomeItemId>): HomeUiState {
        // Resetting position must not turn replay into a screen-triggered fetch.
        return state.copy(
            feed = eligible(state.loadedItems, excluded),
            currentIndex = 0,
            deckGeneration = state.deckGeneration + 1,
            prefetchEnabled = false
        )
    }

    fun acceptPage(state: HomeUiState, page: HomeFeedPage, excluded: Set<HomeItemId>): HomeUiState {
        val existing = state.loadedItems.mapTo(mutableSetOf()) { it.id }
        val added = page.items.filter { existing.add(it.id) }
        emptyPages = if (eligible(page.items, excluded).isEmpty()) emptyPages + 1 else 0
        val loaded = state.loadedItems + added
        return state.copy(
            loadedItems = loaded,
            feed = eligible(loaded, excluded),
            nextCursor = page.nextCursor,
            hasMore = page.nextCursor != null && emptyPages < MAX_EMPTY_PAGES
        )
    }

    fun shouldContinue(state: HomeUiState): Boolean = emptyPages > 0 && state.hasMore

    private companion object {
        const val MAX_EMPTY_PAGES = 3
    }
}
