package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.domain.marketplace.application.ReportedListingVisibilityPreferenceStore
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReportedListingVisibilityPreferenceViewModel(
    private val preferenceStore: ReportedListingVisibilityPreferenceStore
) : ViewModel() {
    private val mutableShouldHideReportedListings = MutableStateFlow(true)
    val shouldHideReportedListings = mutableShouldHideReportedListings.asStateFlow()
    private var currentReporterId: MarketplaceReporterId? = null
    private var accountGeneration = 0

    fun setAccount(reporterId: MarketplaceReporterId?) {
        if (currentReporterId == reporterId) return
        currentReporterId = reporterId
        accountGeneration++
        val generation = accountGeneration
        mutableShouldHideReportedListings.value = true
        reporterId?.let { id ->
            viewModelScope.launch {
                val preference = preferenceStore.shouldHideReportedListings(id)
                if (generation == accountGeneration) {
                    mutableShouldHideReportedListings.value = preference
                }
            }
        }
    }

    fun setHideReportedListings(shouldHide: Boolean, onSaved: () -> Unit = {}) {
        val reporterId = currentReporterId ?: return
        mutableShouldHideReportedListings.value = shouldHide
        viewModelScope.launch {
            preferenceStore.setHideReportedListings(reporterId, shouldHide)
            if (currentReporterId == reporterId) onSaved()
        }
    }
}
