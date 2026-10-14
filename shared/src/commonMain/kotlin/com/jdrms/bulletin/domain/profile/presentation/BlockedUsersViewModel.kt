package com.jdrms.bulletin.domain.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.RefreshSignal
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.BlockedUser
import com.jdrms.bulletin.domain.profile.application.GetBlockedUsers
import com.jdrms.bulletin.domain.profile.application.UnblockUser
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BlockedUsersUiState(
    val users: List<BlockedUser> = emptyList(),
    val isLoading: Boolean = false,
    val removingUserId: String? = null,
    val errorMessage: String? = null
)

class BlockedUsersViewModel(
    private val getBlockedUsers: GetBlockedUsers,
    private val unblockUser: UnblockUser,
    private val listingChangedSignal: RefreshSignal? = null
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(BlockedUsersUiState())
    val uiState = mutableUiState.asStateFlow()
    private var loadJob: Job? = null
    private var unblockJob: Job? = null

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                when (val result = getBlockedUsers()) {
                    is Result.Success -> mutableUiState.update {
                        it.copy(users = result.data, isLoading = false, errorMessage = null)
                    }
                    is Result.Error -> mutableUiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableUiState.update {
                    it.copy(isLoading = false, errorMessage = "Unable to load blocked users. Please try again.")
                }
            }
        }
    }

    fun unblock(userId: String) {
        if (mutableUiState.value.removingUserId != null) return
        if (mutableUiState.value.users.none { it.id.value == userId }) return
        unblockJob?.cancel()
        unblockJob = viewModelScope.launch {
            mutableUiState.update { it.copy(removingUserId = userId, errorMessage = null) }
            try {
                when (val result = unblockUser(UserId(userId))) {
                    is Result.Success -> mutableUiState.update {
                        it.copy(
                            users = it.users.filterNot { user -> user.id.value == userId },
                            removingUserId = null
                        )
                    }.also { listingChangedSignal?.emit() }
                    is Result.Error -> mutableUiState.update {
                        it.copy(removingUserId = null, errorMessage = result.message)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                mutableUiState.update {
                    it.copy(
                        removingUserId = null,
                        errorMessage = "Unable to unblock this user. Please try again."
                    )
                }
            }
        }
    }
}
