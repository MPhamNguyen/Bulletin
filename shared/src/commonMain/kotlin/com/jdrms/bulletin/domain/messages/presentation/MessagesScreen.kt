package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinMotion
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MessagesScreen(
    viewModel: MessagesViewModel,
    onViewProfile: (String) -> Unit = {},
    onConversationVisibilityChanged: (Boolean) -> Unit = {},
    onReportUser: (String) -> Unit = {}
) {
    DisposableEffect(viewModel) {
        viewModel.onDestinationActiveChanged(true)
        onDispose { viewModel.onDestinationActiveChanged(false) }
    }
    val state by viewModel.uiState.collectAsState()
    val inboxState by viewModel.inboxState.collectAsState()
    val selected = state.conversations.firstOrNull { it.id == state.selectedConversationId }
    val isChat = state.isConversationOpen && selected != null
    androidx.compose.runtime.LaunchedEffect(isChat) {
        onConversationVisibilityChanged(isChat)
    }
    Scaffold(
        // MainDestinationBody already reserves space for the system bars.
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            if (!isChat) TopAppBar(windowInsets = WindowInsets(0.dp), title = { Text("Conversations") })
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Surface(modifier = Modifier.fillMaxSize().padding(padding), color = MaterialTheme.colorScheme.background) {
            AnimatedContent(
                targetState = selected?.id.takeIf { isChat },
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val slide = tween<IntOffset>(BulletinMotion.EMPHASIZED)
                    val enter = fadeIn(tween(BulletinMotion.STANDARD))
                    val exit = fadeOut(tween(BulletinMotion.FAST))
                    if (targetState != null) {
                        (slideInHorizontally(slide) { it } + enter) togetherWith
                            (slideOutHorizontally(slide) { -it / 4 } + exit)
                    } else {
                        (slideInHorizontally(slide) { -it / 4 } + enter) togetherWith
                            (slideOutHorizontally(slide) { it } + exit)
                    }
                },
                label = "messages-destination"
            ) { conversationId ->
                val conversation = conversationId?.let { id ->
                    state.conversations.firstOrNull { it.id == id }
                }
                if (conversation == null) {
                    MessagesInboxContent(state, inboxState, viewModel, Modifier.fillMaxSize())
                } else {
                    MessagesChatContent(
                        state,
                        viewModel,
                        conversation,
                        Modifier.fillMaxSize(),
                        ConversationNavigation(onViewProfile, onReportUser)
                    )
                }
            }
        }
    }
    state.messageReportForm?.let { form ->
        MessageReportScreen(
            form = form,
            isSubmitting = state.isReporting,
            isInboxLoading = inboxState.isLoading,
            callbacks = MessageReportCallbacks(
                onReasonSelected = viewModel.selectMessageReportReason,
                onDescriptionChanged = viewModel.updateMessageReportDescription,
                onSubmit = viewModel.submitMessageReport,
                onDismiss = viewModel.dismissMessageReport
            ),
            errorMessage = form.errorMessage ?: state.errorMessage
        )
    }
}

internal fun conversationTitle(conversation: Conversation, viewerId: SenderId?): String {
    val otherNames = viewerId?.let(conversation::otherParticipantNames).orEmpty()
    return otherNames.joinToString(", ").ifBlank { "Conversation" }
}

internal fun conversationPreviewText(conversation: Conversation): String =
    conversation.lastMessage?.let { messageBodyText(it, isRevealed = false) } ?: "No messages yet"

internal fun messageBodyText(message: Message, isRevealed: Boolean): String =
    if (message.isReported && !isRevealed) "Reported message hidden" else message.content

@Composable
internal fun ConversationAvatar(name: String, modifier: Modifier = Modifier) {
    val initials = name.trim()
        .split(Regex("\\s+"))
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
    Surface(
        modifier = modifier.size(40.dp).semantics { contentDescription = "$name avatar" },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            Text(initials.ifBlank { "?" }, style = MaterialTheme.typography.labelLarge)
        }
    }
}
