package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.application.RecordSwipeDecision
import com.jdrms.bulletin.domain.home.application.SaveListingPort
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.SwipeDecision
import com.jdrms.bulletin.domain.home.domain.model.SwipeVerdict
import com.jdrms.bulletin.domain.home.domain.repository.SwipeDecisionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Retry ownership outlives a card, but never a refresh, replay or session boundary. */
internal class HomeSaveInteraction(
    private val store: HomeInteractionStore,
    private val record: RecordSwipeDecision,
    private val decisions: SwipeDecisionRepository,
    private val save: SaveListingPort?
) {
    private data class Attempt(
        val viewer: String,
        val lifecycle: Long,
        var failed: Boolean = false,
        var job: Job? = null
    )
    private val attempts = mutableMapOf<HomeItemId, Attempt>()
    private var passJob: Job? = null

    fun saveCurrent() {
        if (!store.interaction.saveAvailable) return
        val capture = store.capture() ?: return
        val viewer = capture.viewer ?: return
        if (capture.listingId in attempts || !store.transition(CardEvent.COMMIT)) return
        store.requestDeparture(HomeDepartureKind.SAVE)
        val attempt = Attempt(viewer, store.lifecycle)
        attempts[capture.listingId] = attempt
        start(capture.listingId, attempt)
    }

    fun passCurrent() {
        val capture = store.capture() ?: return
        if (!store.transition(CardEvent.COMMIT)) return
        store.requestDeparture(HomeDepartureKind.PASS)
        passJob = store.scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val result = interactionAttempt {
                record(
                    capture.viewer,
                    SwipeDecision(capture.listingId, SwipeVerdict.PASS)
                )
            }
            if (result is Result.Error && capture.viewer != null && store.matches(capture)) {
                store.edit { it.copy(tray = it.tray.copy(error = HomeInteractionError.DECISION_STORAGE)) }
            }
        }
    }

    fun retry(listingId: HomeItemId) {
        val attempt = attempts[listingId] ?: return
        if (!attempt.failed || !isCurrent(attempt)) return
        attempt.failed = false
        removeEffect(listingId)
        start(listingId, attempt)
    }

    fun dismiss(listingId: HomeItemId) {
        val attempt = attempts[listingId] ?: return
        if (!attempt.failed || !isCurrent(attempt)) return
        attempts.remove(listingId)
        removeEffect(listingId)
        removeExclusion(listingId, attempt)
    }

    fun reset() {
        passJob?.cancel()
        passJob = null
        val abandoned = attempts.toMap()
        attempts.clear()
        abandoned.forEach { (id, attempt) ->
            attempt.job?.cancel()
            if (attempt.failed) removeExclusion(id, attempt)
        }
    }

    private fun start(id: HomeItemId, attempt: Attempt) {
        attempt.job = store.scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                val recorded = interactionAttempt { record(attempt.viewer, SwipeDecision(id, SwipeVerdict.SAVE)) }
                if (!isCurrent(attempt) || attempts[id] !== attempt) return@launch
                val result = if (recorded is Result.Success) {
                    interactionAttempt { requireNotNull(save).save(attempt.viewer, id) }
                } else {
                    recorded
                }
                if (!isCurrent(attempt) || attempts[id] !== attempt) return@launch
                when (result) {
                    is Result.Success -> attempts.remove(id)
                    is Result.Error -> offerRetry(id, attempt)
                }
            } catch (cancellation: CancellationException) {
                if (attempts[id] === attempt) attempts.remove(id)
                throw cancellation
            }
        }
    }

    private fun isCurrent(attempt: Attempt): Boolean =
        attempt.viewer == store.viewer && attempt.lifecycle == store.lifecycle

    private fun offerRetry(id: HomeItemId, attempt: Attempt) {
        attempt.failed = true
        store.edit { it.copy(pendingEffects = it.pendingEffects + HomeEffect.SaveRetryOffered(store.nextId(), id)) }
    }

    private fun removeEffect(id: HomeItemId) {
        store.edit { state ->
            state.copy(
                pendingEffects = state.pendingEffects.filterNot {
                    it is HomeEffect.SaveRetryOffered && it.listingId == id
                }
            )
        }
    }

    private fun removeExclusion(id: HomeItemId, attempt: Attempt) {
        val capture = store.capture()
        store.scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val result = interactionAttempt { decisions.removeSave(attempt.viewer, id) }
            if (!isCurrent(attempt) || capture == null) return@launch
            if (result is Result.Error && capture.listingId == id && store.matches(capture)) {
                store.edit { it.copy(tray = it.tray.copy(error = HomeInteractionError.DECISION_STORAGE)) }
            }
        }
    }
}
