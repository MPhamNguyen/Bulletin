package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Shared presentation state and identity guards; no transport values enter public state. */
internal class HomeInteractionStore(private val canSave: Boolean, private val canMessage: Boolean) {
    val state = MutableStateFlow(HomeUiState())
    lateinit var scope: CoroutineScope
    var viewer: String? = null
        private set
    var epoch = 0L
        private set
    var lifecycle = 0L
        private set
    var sendJob: Job? = null
    private var sequence = 0L
    val interaction get() = state.value.interaction

    fun nextId(): Long = ++sequence

    fun changeViewer(id: String?) {
        viewer = id
        reset(clearEffects = true)
    }

    fun reset(clearEffects: Boolean = false) {
        epoch++
        if (clearEffects) lifecycle++
        sendJob?.cancel()
        sendJob = null
        val listingId = state.value.feed.getOrNull(state.value.currentIndex)?.id
        edit {
            HomeInteractionState(
                listingId = listingId,
                pendingEffects = if (clearEffects) emptyList() else it.pendingEffects,
                accountNotice = if (viewer == null) HomeInteractionError.NOT_SIGNED_IN else null,
                saveAvailable = listingId != null && viewer != null && canSave,
                messageAvailable = listingId != null && viewer != null && canMessage
            )
        }
    }

    fun syncListing() {
        if (state.value.feed.getOrNull(state.value.currentIndex)?.id != interaction.listingId) reset()
    }

    fun edit(transform: (HomeInteractionState) -> HomeInteractionState) {
        state.update { it.copy(interaction = transform(it.interaction)) }
    }

    fun transition(event: CardEvent): Boolean = when (
        val result = CardStateMachine.transition(
            interaction.card,
            event
        )
    ) {
        is CardTransition.Rejected -> false
        is CardTransition.Accepted -> {
            edit { it.copy(card = result.next) }
            true
        }
    }

    fun capture(): HomeActionCapture? {
        val listingId = interaction.listingId ?: return null
        return HomeActionCapture(listingId, viewer, epoch)
    }

    fun matches(capture: HomeActionCapture): Boolean =
        capture.epoch == epoch && capture.viewer == viewer && capture.listingId == interaction.listingId

    fun requestDeparture(kind: HomeDepartureKind) {
        val listingId = interaction.listingId ?: return
        edit { it.copy(departure = HomeDeparture(nextId(), listingId, kind)) }
    }

    /** Appends may filter earlier decisions, but cannot remove the card awaiting its exit callback. */
    fun reconcilePage(previous: HomeUiState, next: HomeUiState): HomeUiState {
        val before = previous.feed.take(previous.currentIndex).map { it.id }.toSet()
        val index = next.feed.count { it.id in before }
        val current = previous.feed.getOrNull(previous.currentIndex) ?: return next.copy(currentIndex = index)
        val feed = if (interaction.card == CardState.Leaving && next.feed.none { it.id == current.id }) {
            next.feed.toMutableList().apply { add(index, current) }
        } else {
            next.feed
        }
        val currentIndex = feed.indexOfFirst { it.id == current.id }.takeIf { it >= 0 } ?: index
        return next.copy(feed = feed, currentIndex = currentIndex)
    }
}

internal data class HomeActionCapture(val listingId: HomeItemId, val viewer: String?, val epoch: Long)
