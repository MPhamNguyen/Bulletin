package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.GetConversations
import com.jdrms.bulletin.domain.messages.application.InboxIdentity
import com.jdrms.bulletin.domain.messages.application.ObserveInboxInvalidations
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.InboxInvalidation
import com.jdrms.bulletin.domain.messages.domain.service.InboxReconciliation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class InboxState(
    val viewerId: SenderId? = null,
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val connectionState: InboxConnectionState = InboxConnectionState.CONNECTING
)

enum class InboxConnectionState { CONNECTING, CONNECTED, DISCONNECTED }

/** One inbox snapshot and one user-topic subscription for the signed-in app session. */
class InboxStateOwner(
    private val getConversations: GetConversations,
    private val observeInboxInvalidations: ObserveInboxInvalidations,
    private val identity: InboxIdentity,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private val mutableState = MutableStateFlow(InboxState())
    val state: StateFlow<InboxState> = mutableState.asStateFlow()
    private val refreshMutex = Mutex()
    private var observationJob: Job? = null
    private val sessionJob = scope.launch {
        identity.observeUserIds().distinctUntilChanged().collect(::setSession)
    }

    private fun setSession(userId: SenderId?) {
        if (state.value.viewerId == userId && (userId != null || !state.value.isLoading)) return
        observationJob?.cancel()
        observationJob = null
        mutableState.value = InboxState(
            viewerId = userId,
            isLoading = userId != null,
            errorMessage = if (userId == null) "Sign in to access your messages." else null,
            connectionState = if (userId == null) InboxConnectionState.DISCONNECTED else InboxConnectionState.CONNECTING
        )
        if (userId != null) {
            observationJob = launchObservation(userId)
            scope.launch { refreshFor(userId) }
        }
    }

    fun refresh() {
        val previousUserId = state.value.viewerId
        if (!synchronizeIdentity()) return
        val userId = state.value.viewerId ?: return
        if (previousUserId == userId) scope.launch { refreshFor(userId) }
    }

    fun synchronizeIdentity(): Boolean {
        val userId = identity.currentUserId
        if (state.value.viewerId != userId) setSession(userId)
        return userId != null
    }

    fun onForeground() {
        refresh()
        val userId = state.value.viewerId ?: return
        if (observationJob?.isActive != true) {
            mutableState.update { it.copy(connectionState = InboxConnectionState.CONNECTING) }
            observationJob = launchObservation(userId)
        }
    }

    fun recordMessage(message: Message): Result<Unit> = updateConversation(message.conversationId) { conversation ->
        conversation.recordMessage(message)
    }

    fun reconcileMessages(conversationId: ConversationId, messages: List<Message>): Result<Unit> =
        updateConversation(conversationId) { conversation -> conversation.reconcileMessages(messages) }

    fun recordReadAt(conversationId: ConversationId, readerId: SenderId, readAt: kotlin.time.Instant): Result<Unit> =
        updateConversation(conversationId) { conversation -> conversation.recordReadAt(readerId, readAt) }

    private fun updateConversation(
        conversationId: ConversationId,
        change: (Conversation) -> Result<Conversation>
    ): Result<Unit> {
        var outcome: Result<Unit> = Result.Success(Unit)
        mutableState.update { snapshot ->
            val existing = snapshot.conversations.firstOrNull { it.id == conversationId } ?: return@update snapshot
            when (val changed = change(existing)) {
                is Result.Error -> {
                    outcome = changed
                    snapshot
                }
                is Result.Success -> snapshot.copy(
                    conversations = snapshot.conversations.map { current ->
                        if (current.id == conversationId) changed.data else current
                    }.sortedByDescending(Conversation::updatedAtMillis)
                )
            }
        }
        return outcome
    }

    private fun launchObservation(userId: SenderId): Job = scope.launch {
        observeInboxInvalidations(userId).collectLatest { invalidation ->
            when (invalidation) {
                InboxInvalidation.Subscribed -> {
                    if (state.value.viewerId == userId) {
                        mutableState.update { it.copy(connectionState = InboxConnectionState.CONNECTED) }
                    }
                    refreshFor(userId)
                }
                is InboxInvalidation.Changed -> {
                    delay(INBOX_COALESCE_MILLIS)
                    refreshFor(userId)
                }
                InboxInvalidation.Disconnected, is InboxInvalidation.Failed -> {
                    if (state.value.viewerId == userId) {
                        mutableState.update { it.copy(connectionState = InboxConnectionState.DISCONNECTED) }
                    }
                }
            }
        }
    }

    private suspend fun refreshFor(userId: SenderId) = refreshMutex.withLock {
        if (state.value.viewerId != userId) return@withLock
        mutableState.update { it.copy(isLoading = true) }
        val result = getConversations()
        if (state.value.viewerId != userId) return@withLock
        when (result) {
            is Result.Success -> if (result.data.viewerId == userId) {
                mutableState.update { snapshot ->
                    snapshot.copy(
                        conversations = InboxReconciliation.merge(snapshot.conversations, result.data.conversations),
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } else {
                mutableState.update {
                    it.copy(
                        conversations = emptyList(),
                        isLoading = false,
                        errorMessage = "Unable to complete the messaging request. Please try again."
                    )
                }
            }
            is Result.Error -> mutableState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Unable to complete the messaging request. Please try again."
                )
            }
        }
    }

    fun close() {
        sessionJob.cancel()
        observationJob?.cancel()
        scope.cancel()
        mutableState.value = InboxState(isLoading = false)
    }
}

private const val INBOX_COALESCE_MILLIS = 300L
