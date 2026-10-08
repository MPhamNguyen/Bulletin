package com.jdrms.bulletin.domain.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.GetProfileActivity
import com.jdrms.bulletin.domain.profile.application.GetProfileOverview
import com.jdrms.bulletin.domain.profile.application.ProfileSoldListingsProvider
import com.jdrms.bulletin.domain.profile.application.SessionRepository
import com.jdrms.bulletin.domain.profile.application.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Read model for the profile landing destination. */
class ProfileViewModel(
    private val sessionRepository: SessionRepository,
    private val getProfileOverview: GetProfileOverview,
    private val getProfileActivity: GetProfileActivity,
    listingChangedSignal: RefreshSignal,
    private val soldListingsProvider: ProfileSoldListingsProvider? = null
) : ViewModel() {
    private val state = MutableStateFlow(ProfileUiState())
    val uiState = state.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.state.collect { session ->
                when (session) {
                    is SessionState.Authenticated -> {
                        state.update { it.copy(profile = session.profile) }
                        refreshProfileDetails()
                    }
                    SessionState.Checking -> Unit
                    SessionState.Unauthenticated -> state.value = ProfileUiState()
                }
            }
        }
        viewModelScope.launch { listingChangedSignal.events.collect { refreshActiveListings() } }
    }

    fun refreshActiveListings() {
        val profile = state.value.profile ?: return
        viewModelScope.launch {
            val activeCount = getProfileActivity(profile.id)
            val soldCount = soldListingsProvider?.getSoldListingsCount(profile.id)
            state.update { it.copy(activeListingsCount = activeCount, itemsSoldCount = soldCount) }
        }
    }

    private suspend fun refreshProfileDetails() {
        val profile = state.value.profile ?: return
        when (val result = getProfileOverview(profile.id)) {
            is Result.Success -> result.data?.let { overview ->
                state.update {
                    it.copy(
                        profile = overview.profile,
                        reputation = overview.reputation,
                        activeListingsCount = overview.activeListingsCount,
                        itemsSoldCount = soldListingsProvider?.getSoldListingsCount(profile.id)
                    )
                }
            }
            is Result.Error -> state.update {
                it.copy(errorMessage = result.exception.message ?: "Failed to load profile")
            }
        }
    }
}
