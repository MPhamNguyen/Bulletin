package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedPage
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId

internal enum class HomeDeckExclusion { PASS, SAVE }

/** Session-local seam for the decision storage introduced by later Home features. */
internal class HomeDeckPolicy {
    private var userId: String? = null
    private val exclusions = mutableMapOf<String, MutableMap<HomeItemId, HomeDeckExclusion>>()
    private var emptyPages = 0

    fun changeAccount(id: String?) {
        exclusions.clear()
        userId = id
        emptyPages = 0
    }

    fun resetPagination() {
        emptyPages = 0
    }

    fun exclude(id: HomeItemId, exclusion: HomeDeckExclusion) {
        val owner = userId ?: return
        exclusions.getOrPut(owner) { mutableMapOf() }[id] = exclusion
    }

    fun eligible(items: List<HomeFeedItem>): List<HomeFeedItem> {
        val excluded = exclusions[userId].orEmpty()
        return items.filterNot { it.id in excluded }
    }

    fun replay(state: HomeUiState): HomeUiState {
        exclusions[userId]?.entries?.removeAll { it.value == HomeDeckExclusion.PASS }
        // Resetting position must not turn replay into a screen-triggered fetch.
        return state.copy(
            feed = eligible(state.loadedItems),
            currentIndex = 0,
            deckGeneration = state.deckGeneration + 1,
            prefetchEnabled = false
        )
    }

    fun acceptPage(state: HomeUiState, page: HomeFeedPage): HomeUiState {
        val existing = state.loadedItems.mapTo(mutableSetOf()) { it.id }
        val added = page.items.filter { existing.add(it.id) }
        emptyPages = if (eligible(page.items).isEmpty()) emptyPages + 1 else 0
        val loaded = state.loadedItems + added
        return state.copy(
            loadedItems = loaded,
            feed = eligible(loaded),
            nextCursor = page.nextCursor,
            hasMore = page.nextCursor != null && emptyPages < MAX_EMPTY_PAGES
        )
    }

    fun shouldContinue(state: HomeUiState): Boolean = emptyPages > 0 && state.hasMore

    private companion object {
        const val MAX_EMPTY_PAGES = 3
    }
}
