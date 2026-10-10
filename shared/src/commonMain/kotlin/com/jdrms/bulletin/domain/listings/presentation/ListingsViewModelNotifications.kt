package com.jdrms.bulletin.domain.listings.presentation

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun ListingsViewModel.showFlashNotification(message: String) {
    flashNotificationJob?.cancel()
    _uiState.update { it.copy(successMessage = message) }
    flashNotificationJob = notificationScope.launch {
        delay(ListingsViewModel.FLASH_NOTIFICATION_DURATION_MILLIS)
        _uiState.update { state ->
            if (state.successMessage == message) state.copy(successMessage = null) else state
        }
    }
}
