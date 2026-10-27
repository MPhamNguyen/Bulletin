package com.jdrms.bulletin.app.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId

internal class MutableMainNavigationState {
    var currentDestination by mutableStateOf(AppDestination.HOME)
    var profileDestination by mutableStateOf(ProfileDestination.PROFILE)
    var editReturnDestination by mutableStateOf(ProfileDestination.PROFILE)
    var pendingConversationId by mutableStateOf<ConversationId?>(null)
    var pendingSellerProfileId by mutableStateOf<String?>(null)

    val showsBottomNavigation: Boolean
        get() = snapshot().showsBottomNavigation

    fun snapshot() = NavigationSnapshot(currentDestination, profileDestination)

    fun apply(snapshot: NavigationSnapshot) {
        currentDestination = snapshot.currentDestination
        profileDestination = snapshot.profileDestination
    }
}
