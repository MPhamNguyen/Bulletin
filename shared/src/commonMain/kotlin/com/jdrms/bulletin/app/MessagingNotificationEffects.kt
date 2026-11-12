package com.jdrms.bulletin.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jdrms.bulletin.app.di.MessagesModule
import com.jdrms.bulletin.app.navigation.AppDestination
import com.jdrms.bulletin.app.navigation.MutableMainNavigationState
import com.jdrms.bulletin.domain.messages.presentation.MessagesViewModel

/** Connects message notifications to navigation: reports what is on screen and opens tapped conversations. */
@Composable
internal fun MessagingNotificationEffects(
    messages: MessagesModule,
    navigation: MutableMainNavigationState,
    viewModel: MessagesViewModel
) {
    val ui by viewModel.uiState.collectAsState()
    val inbox by viewModel.inboxState.collectAsState()
    val request by messages.notificationRequests.pending.collectAsState()
    val lookingAt = ui.selectedConversationId.takeIf {
        navigation.currentDestination == AppDestination.MESSAGES && ui.isConversationOpen
    }
    LaunchedEffect(lookingAt) { messages.activeConversation.setOpenConversation(lookingAt) }
    DisposableEffect(Unit) { onDispose { messages.activeConversation.setOpenConversation(null) } }
    LaunchedEffect(request, inbox.viewerId) {
        val conversationId = messages.notificationRequests.take(inbox.viewerId) ?: return@LaunchedEffect
        viewModel.openConversation(conversationId)
        navigation.apply(navigation.snapshot().selectBottomNavigationDestination(AppDestination.MESSAGES))
    }
}
