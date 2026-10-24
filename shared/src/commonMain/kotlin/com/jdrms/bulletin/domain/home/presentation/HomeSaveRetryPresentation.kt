package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId

internal val LocalHomeSaveRetryPresentation = staticCompositionLocalOf<HomeSaveRetryPresentation?> { null }

internal data class HomeRetryOffer(val effect: HomeEffect.SaveRetryOffered, val shown: Boolean = false)

/** UI delivery bookkeeping survives recreation; only an explicit destination change dismisses offers. */
internal class HomeSaveRetryPresentation {
    var offers by mutableStateOf<List<HomeRetryOffer>>(emptyList())
        private set
    private var generation = -1L

    fun synchronize(isHome: Boolean, state: HomeUiState, actions: HomeCardActions) {
        if (generation != state.deckGeneration) {
            offers = emptyList()
            generation = state.deckGeneration
        }
        state.interaction.pendingEffects.filterIsInstance<HomeEffect.SaveRetryOffered>().forEach { effect ->
            if (offers.none { it.effect.id == effect.id }) offers = offers + HomeRetryOffer(effect)
        }
        if (!isHome) {
            val abandoned = offers
            offers = emptyList()
            abandoned.forEach { actions.dismissSaveRetry(it.effect.listingId) }
        }
    }

    fun shown(id: Long): Boolean {
        if (offers.none { it.effect.id == id && !it.shown }) return false
        offers = offers.map { if (it.effect.id == id) it.copy(shown = true) else it }
        return true
    }

    fun take(id: Long): HomeRetryOffer? {
        val offer = offers.firstOrNull { it.effect.id == id } ?: return null
        offers = offers.filterNot { it.effect.id == id }
        return offer
    }

    internal fun snapshot(): List<Any> = listOf(generation) + offers.flatMap {
        listOf(it.effect.id, it.effect.listingId.value, it.shown)
    }

    companion object {
        val saver: Saver<HomeSaveRetryPresentation, List<Any>> = Saver(
            save = { it.snapshot() },
            restore = { restore(it) }
        )

        internal fun restore(snapshot: List<Any>): HomeSaveRetryPresentation = HomeSaveRetryPresentation().apply {
            generation = snapshot.first() as Long
            offers = snapshot.drop(1).chunked(3).map {
                val effect = HomeEffect.SaveRetryOffered(it[0] as Long, HomeItemId(it[1] as String))
                HomeRetryOffer(effect, it[2] as Boolean)
            }
        }
    }
}
