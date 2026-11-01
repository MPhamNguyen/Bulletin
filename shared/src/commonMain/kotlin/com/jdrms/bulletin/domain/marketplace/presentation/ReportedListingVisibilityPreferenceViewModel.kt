package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.domain.marketplace.application.ReportedListingVisibilityPreferenceStore
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ReportedListingVisibilityPreferenceViewModel(
    private val preferenceStore: ReportedListingVisibilityPreferenceStore
) : ViewModel() {
    private val mutableShouldHideReportedListings = MutableStateFlow(true)
    val shouldHideReportedListings = mutableShouldHideReportedListings.asStateFlow()
    private var currentReporterId: MarketplaceReporterId? = null
    private var accountGeneration = 0
    private var preferenceGeneration = 0
    private var persistedPreference = true
    private val saveMutex = Mutex()

    fun setAccount(reporterId: MarketplaceReporterId?) {
        if (currentReporterId == reporterId) return
        currentReporterId = reporterId
        accountGeneration++
        preferenceGeneration++
        val generation = accountGeneration
        val loadGeneration = preferenceGeneration
        persistedPreference = true
        mutableShouldHideReportedListings.value = true
        reporterId?.let { id ->
            viewModelScope.launch {
                try {
                    val preference = preferenceStore.shouldHideReportedListings(id)
                    if (isCurrent(id, generation) && loadGeneration == preferenceGeneration) {
                        persistedPreference = preference
                        mutableShouldHideReportedListings.value = preference
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Keep the safe default when the stored preference cannot be read.
                }
            }
        }
    }

    fun setHideReportedListings(shouldHide: Boolean, onSaved: () -> Unit = {}) {
        val reporterId = currentReporterId ?: return
        val generation = accountGeneration
        val changeGeneration = ++preferenceGeneration
        mutableShouldHideReportedListings.value = shouldHide
        viewModelScope.launch {
            try {
                saveMutex.withLock {
                    if (!isCurrentPreference(reporterId, generation, changeGeneration)) return@withLock
                    preferenceStore.setHideReportedListings(reporterId, shouldHide)
                    if (isCurrent(reporterId, generation)) persistedPreference = shouldHide
                    if (isCurrentPreference(reporterId, generation, changeGeneration)) onSaved()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (isCurrentPreference(reporterId, generation, changeGeneration)) {
                    mutableShouldHideReportedListings.value = persistedPreference
                }
            }
        }
    }

    private fun isCurrent(reporterId: MarketplaceReporterId, generation: Int) =
        currentReporterId == reporterId && accountGeneration == generation

    private fun isCurrentPreference(
        reporterId: MarketplaceReporterId,
        generation: Int,
        changeGeneration: Int
    ) = isCurrent(reporterId, generation) && preferenceGeneration == changeGeneration
}
