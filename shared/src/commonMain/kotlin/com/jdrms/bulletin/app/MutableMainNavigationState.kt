package com.jdrms.bulletin.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.app.navigation.ProfileDestination
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId

internal class MainNavigationState {
    var currentDestination by mutableStateOf(AppDestination.HOME)
    var profileDestination by mutableStateOf(ProfileDestination.PROFILE)
    var editReturnDestination by mutableStateOf(ProfileDestination.PROFILE)
    var pendingConversationId by mutableStateOf<ConversationId?>(null)

    val showsBottomNavigation: Boolean
        get() = snapshot().showsBottomNavigation

    fun snapshot() = com.jdrms.bulletin.app.navigation.MainNavigationState(currentDestination, profileDestination)

    fun apply(snapshot: com.jdrms.bulletin.app.navigation.MainNavigationState) {
        currentDestination = snapshot.currentDestination
        profileDestination = snapshot.profileDestination
    }
}
