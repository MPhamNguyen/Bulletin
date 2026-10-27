package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.BlockMarketplaceUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MarketplaceUserBlockUiState(
    val blockedUserId: String? = null,
    val isBlocking: Boolean = false,
    val errorMessage: String? = null,
    val blocked: Boolean = false
)

class MarketplaceUserBlockViewModel(
    private val blockMarketplaceUser: BlockMarketplaceUser,
    private val listingChangedSignal: RefreshSignal? = null
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(MarketplaceUserBlockUiState())
    val uiState = mutableUiState.asStateFlow()
    private var blockGeneration = 0

    fun begin(userId: String) {
        blockGeneration++
        mutableUiState.value = MarketplaceUserBlockUiState(blockedUserId = userId)
    }

    fun reset() {
        blockGeneration++
        mutableUiState.value = MarketplaceUserBlockUiState()
    }

    fun submit() {
        val userId = mutableUiState.value.blockedUserId ?: return
        if (mutableUiState.value.isBlocking || mutableUiState.value.blocked) return
        val generation = blockGeneration
        mutableUiState.update { it.copy(isBlocking = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                when (val result = blockMarketplaceUser(userId)) {
                    is Result.Success -> {
                        listingChangedSignal?.emit()
                        updateIfCurrent(generation, userId) { it.copy(isBlocking = false, blocked = true) }
                    }
                    is Result.Error -> updateIfCurrent(generation, userId) {
                        it.copy(
                            isBlocking = false,
                            errorMessage = result.message
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                updateIfCurrent(generation, userId) {
                    it.copy(isBlocking = false, errorMessage = "Unable to block this user. Please try again.")
                }
            }
        }
    }

    private fun updateIfCurrent(
        generation: Int,
        userId: String,
        update: (MarketplaceUserBlockUiState) -> MarketplaceUserBlockUiState
    ) {
        if (generation == blockGeneration && mutableUiState.value.blockedUserId == userId) {
            mutableUiState.update(update)
        }
    }
}
