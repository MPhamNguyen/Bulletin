package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceLayoutPreference
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceLayoutPreferenceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class MarketplaceLayoutPreferenceController(
    private val uiState: MutableStateFlow<MarketplaceUiState>,
    private val scope: CoroutineScope,
    private val store: MarketplaceLayoutPreferenceStore
) {
    private var job: Job? = null
    private var persistedPreference = MarketplaceLayoutPreference.TWO_PER_ROW

    fun load() {
        job = scope.launch {
            runCatching { store.get() }
                .onSuccess { preference ->
                    persistedPreference = preference
                    uiState.update { it.copy(layoutPreference = preference) }
                }
                .onFailure { it.rethrowCancellation() }
        }
    }

    fun change(preference: MarketplaceLayoutPreference) {
        job?.cancel()
        uiState.update { it.copy(layoutPreference = preference) }
        job = scope.launch {
            val result = runCatching { store.set(preference) }
            result.exceptionOrNull()?.rethrowCancellation()
            if (result.isSuccess) {
                persistedPreference = preference
            } else {
                uiState.update { state ->
                    if (state.layoutPreference == preference) {
                        state.copy(layoutPreference = persistedPreference)
                    } else {
                        state
                    }
                }
            }
        }
    }
}
