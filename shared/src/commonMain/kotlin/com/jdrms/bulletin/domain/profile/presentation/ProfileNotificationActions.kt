package com.jdrms.bulletin.domain.profile.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Owns one-shot profile notifications independently from profile workflow state. */
class ProfileNotificationActions internal constructor(
    private val uiState: MutableStateFlow<ProfileUiState>,
    private val scope: CoroutineScope
) {
    fun clear() {
        cancel()
        uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun cancel() {
        notificationJob?.cancel()
        notificationJob = null
    }

    fun show(message: String) {
        cancel()
        uiState.update { it.copy(successMessage = message) }
        notificationJob = scope.launch {
            delay(ProfileViewModel.FLASH_NOTIFICATION_DURATION_MILLIS)
            uiState.update { current ->
                if (current.successMessage == message) current.copy(successMessage = null) else current
            }
        }
    }

    private var notificationJob: Job? = null
}
