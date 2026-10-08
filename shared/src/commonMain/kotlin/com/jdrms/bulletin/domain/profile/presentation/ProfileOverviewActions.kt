package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.ManageProfile
import com.jdrms.bulletin.domain.profile.application.ProfileActiveListingsProvider
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileOverviewActions internal constructor(
    private val uiState: MutableStateFlow<ProfileUiState>,
    private val scope: CoroutineScope,
    private val manageProfile: ManageProfile,
    private val activeListingsProvider: ProfileActiveListingsProvider?,
    listingChangedSignal: RefreshSignal?
) {
    init { scope.launch { listingChangedSignal?.events?.collect { refreshActiveListings() } } }

    fun loadProfile(userId: UserId) = scope.launch {
        uiState.update { it.copy(isLoading = true, errorMessage = null) }
        when (val result = manageProfile.getProfile(userId)) {
            is Result.Success -> {
                val profile = result.data
                val reputation = manageProfile.getReputation(userId)
                val count = profile?.let { activeListingsProvider?.getActiveListingsCount(it.id) } ?: 0
                uiState.update {
                    it.copy(
                        profile = profile,
                        profileDraft = profile?.let(ProfileDraft::from) ?: ProfileDraft(),
                        reputation = reputation,
                        activeListingsCount = count,
                        isLoading = false
                    )
                }
            }
            is Result.Error -> uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = result.exception.message ?: "Failed to load profile"
                )
            }
        }
    }

    fun refreshActiveListings() = scope.launch {
        val profile = uiState.value.profile ?: return@launch
        val count = activeListingsProvider?.getActiveListingsCount(profile.id) ?: 0
        uiState.update { it.copy(activeListingsCount = count) }
    }
}
