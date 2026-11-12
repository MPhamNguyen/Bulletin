package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceInterestNotifier
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceInterestPreferenceStore
import com.jdrms.bulletin.domain.marketplace.application.NoOpMarketplaceInterestNotifier
import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterest
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterestOwnerId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MarketplaceInterestsViewModel(
    private val preferenceStore: MarketplaceInterestPreferenceStore,
    private val searchMarketplace: SearchMarketplace? = null,
    private val listingEvents: MarketplaceInterestListingEvents = MarketplaceInterestListingEvents(),
    private val invalidateListingReads: suspend () -> Unit = {},
    private val notifier: MarketplaceInterestNotifier = NoOpMarketplaceInterestNotifier
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MarketplaceInterestsUiState())
    val uiState = mutableUiState.asStateFlow()
    private val saveMutex = Mutex()
    private var ownerId: MarketplaceInterestOwnerId? = null
    private var ownerGeneration = 0
    private var preferenceGeneration = 0
    private var persistedInterests = emptyMap<MarketplaceCategory, MarketplaceInterest>()
    private var interestsVisible = false
    private var listingObservationJob: Job? = null
    private val matchLoader = MarketplaceInterestMatchLoader(searchMarketplace, invalidateListingReads)

    private fun startListingObservation(owner: MarketplaceInterestOwnerId) {
        listingObservationJob?.cancel()
        listingObservationJob = viewModelScope.launch {
            listingEvents.events.collect { event ->
                if (event.ownerId != owner) return@collect
                matchLoader.invalidate()
                loadMatches(notifyNewMatches = true)
            }
        }
    }

    fun setOwner(id: MarketplaceInterestOwnerId?) {
        if (ownerId == id) return
        notifier.clearAll()
        listingObservationJob?.cancel()
        listingObservationJob = null
        ownerId = id
        ownerGeneration++
        preferenceGeneration++
        persistedInterests = emptyMap()
        mutableUiState.value = MarketplaceInterestsUiState(isLoading = id != null)
        id?.let {
            startListingObservation(it)
            load(it)
        }
    }

    fun toggle(category: MarketplaceCategory) {
        val currentOwner = ownerId ?: return
        val generation = ownerGeneration
        val interests = mutableUiState.value.interests.toggle(category)
        save(currentOwner, generation, interests)
    }

    fun updateInterest(interest: MarketplaceInterest) {
        val currentOwner = ownerId ?: return
        val generation = ownerGeneration
        val interests = mutableUiState.value.interests + (interest.category to interest)
        save(currentOwner, generation, interests)
    }

    private fun save(
        currentOwner: MarketplaceInterestOwnerId,
        generation: Int,
        interests: Map<MarketplaceCategory, MarketplaceInterest>
    ) {
        val changeGeneration = ++preferenceGeneration
        mutableUiState.update { it.copy(interests = interests, isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                saveMutex.withLock {
                    if (!isCurrent(currentOwner, generation, changeGeneration)) return@withLock
                    preferenceStore.set(currentOwner, interests.values.toSet())
                    if (isOwnerCurrent(currentOwner, generation)) persistedInterests = interests
                    if (isCurrent(currentOwner, generation, changeGeneration)) {
                        loadMatches(notifyNewMatches = false)
                    }
                    if (isCurrent(currentOwner, generation, changeGeneration)) {
                        mutableUiState.update { it.copy(isSaving = false) }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (isCurrent(currentOwner, generation, changeGeneration)) {
                    mutableUiState.update {
                        it.copy(
                            interests = persistedInterests,
                            isSaving = false,
                            errorMessage = "Couldn't save interests. Try again."
                        )
                    }
                }
            }
        }
    }

    fun clearError() {
        mutableUiState.update { it.copy(errorMessage = null) }
    }

    fun markMatchesViewed() {
        notifier.clearAll()
        mutableUiState.update { it.copy(unreadMatchCount = 0) }
    }

    fun setInterestsVisible(visible: Boolean) {
        interestsVisible = visible
        if (visible) markMatchesViewed()
    }

    fun refreshMatches() {
        viewModelScope.launch {
            matchLoader.invalidate()
            loadMatches(notifyNewMatches = false)
        }
    }

    private fun load(id: MarketplaceInterestOwnerId) {
        val generation = ownerGeneration
        val loadGeneration = preferenceGeneration
        viewModelScope.launch {
            try {
                val stored = preferenceStore.get(id)
                if (isCurrent(id, generation, loadGeneration)) {
                    val interests = stored.associateBy(MarketplaceInterest::category)
                    persistedInterests = interests
                    mutableUiState.value = MarketplaceInterestsUiState(interests = interests)
                    loadMatches(notifyNewMatches = false)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (isCurrent(id, generation, loadGeneration)) {
                    mutableUiState.value = MarketplaceInterestsUiState(
                        errorMessage = "Couldn't load interests. Try again."
                    )
                }
            }
        }
    }

    private fun isOwnerCurrent(id: MarketplaceInterestOwnerId, generation: Int) =
        ownerId == id && ownerGeneration == generation

    private fun isCurrent(id: MarketplaceInterestOwnerId, generation: Int, changeGeneration: Int) =
        isOwnerCurrent(id, generation) && preferenceGeneration == changeGeneration

    private fun loadMatches(notifyNewMatches: Boolean) {
        val owner = ownerId ?: return
        val interests = mutableUiState.value.interests.values.toList()
        if (interests.isEmpty()) {
            mutableUiState.update { it.copy(matchingListings = emptyList(), unreadMatchCount = 0) }
            return
        }
        val loadOwnerGeneration = ownerGeneration
        val loadPreferenceGeneration = preferenceGeneration
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoadingMatches = true) }
            runCatching { matchLoader.load(owner, interests) }.onSuccess { loaded ->
                if (ownerGeneration != loadOwnerGeneration || preferenceGeneration != loadPreferenceGeneration) {
                    return@onSuccess
                }
                val matches = loaded ?: return@onSuccess
                val previousIds = mutableUiState.value.matchingListings.mapTo(mutableSetOf()) { it.id }
                val newMatches = matches.filter { it.id !in previousIds }
                val newCount = if (notifyNewMatches && !interestsVisible) {
                    newMatches.size
                } else {
                    0
                }
                mutableUiState.update {
                    it.copy(
                        matchingListings = matches,
                        unreadMatchCount = it.unreadMatchCount + newCount,
                        isLoadingMatches = false
                    )
                }
            }.onFailure {
                if (ownerGeneration != loadOwnerGeneration || preferenceGeneration != loadPreferenceGeneration) {
                    return@onFailure
                }
                mutableUiState.update { it.copy(isLoadingMatches = false) }
            }
        }
    }
}

private fun Map<MarketplaceCategory, MarketplaceInterest>.toggle(
    category: MarketplaceCategory
): Map<MarketplaceCategory, MarketplaceInterest> =
    if (category in this) this - category else this + (category to MarketplaceInterest(category))
