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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    private val reviews = InboxReviewLoader(getConversations, mutableState, scope)
    private val sessionJob = scope.launch {
        identity.observeUserIds().distinctUntilChanged().collect(::setSession)
    }

    private fun setSession(userId: SenderId?) {
        if (state.value.viewerId == userId && (userId != null || !state.value.isLoading)) return
        observationJob?.cancel()
        reviews.cancel()
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

    fun recordMessage(message: Message): Result<Unit> {
        val result = updateConversation(message.conversationId) { conversation -> conversation.recordMessage(message) }
        if (result is Result.Error) {
            mutableState.update {
                it.copy(errorMessage = "Message sent, but the conversation could not be updated.")
            }
        }
        return result
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
        observeInboxInvalidations(userId).collect { invalidation ->
            when (invalidation) {
                InboxInvalidation.Subscribed -> {
                    if (state.value.viewerId == userId) {
                        mutableState.update { it.copy(connectionState = InboxConnectionState.CONNECTED) }
                    }
                    refreshFor(userId)
                }
                is InboxInvalidation.Changed -> {
                    refreshConversation(userId, invalidation.conversationId)
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
        val limit = maxOf(10, state.value.conversations.size)
        val result = getConversations(offset = 0, includeReviews = false, limit = limit)
        if (state.value.viewerId != userId) return@withLock
        when (result) {
            is Result.Success -> if (result.data.viewerId == userId) {
                mutableState.update { snapshot ->
                    snapshot.copy(
                        conversations = InboxReconciliation.merge(snapshot.conversations, result.data.conversations),
                        pendingReviewConversationIds = result.data.pendingReviewConversationIds
                            ?: snapshot.pendingReviewConversationIds,
                        unmatchedReviewRequests = result.data.unmatchedReviewRequests
                            ?: snapshot.unmatchedReviewRequests,
                        isLoading = false,
                        hasMore = result.data.conversations.size == limit,
                        errorMessage = null
                    )
                }
                reviews.refresh(userId)
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

    fun loadMore() {
        val snapshot = state.value
        val userId = snapshot.viewerId ?: return
        if (!snapshot.hasMore || snapshot.isLoadingMore || snapshot.isLoading) return
        if (snapshot.errorMessage != null) return
        mutableState.update { it.copy(isLoadingMore = true) }
        scope.launch {
            refreshMutex.withLock {
                if (state.value.viewerId != userId) return@withLock
                val result = getConversations(offset = state.value.conversations.size, includeReviews = false)
                if (state.value.viewerId != userId) return@withLock
                mutableState.update { current ->
                    when (result) {
                        is Result.Success -> current.copy(
                            conversations = (current.conversations + result.data.conversations).distinctBy(
                                Conversation::id
                            ),
                            hasMore = result.data.conversations.size == 10,
                            isLoadingMore = false
                        )
                        is Result.Error -> current.copy(
                            isLoadingMore = false,
                            errorMessage = "Unable to load more conversations."
                        )
                    }
                }
                reviews.refresh(userId)
            }
        }
    }

    fun refreshConversation(id: ConversationId) {
        val userId = state.value.viewerId ?: return
        scope.launch { refreshConversation(userId, id) }
    }

    private suspend fun refreshConversation(userId: SenderId, id: ConversationId) = refreshMutex.withLock {
        val result = getConversations.conversation(userId, id)
        if (state.value.viewerId != userId || result !is Result.Success) return@withLock
        mutableState.update {
            val fetched = it.conversations.filterNot { row -> row.id == id } + listOfNotNull(result.data)
            it.copy(conversations = InboxReconciliation.merge(it.conversations, fetched))
        }
        reviews.refresh(userId)
    }

    fun close() {
        sessionJob.cancel()
        observationJob?.cancel()
        scope.cancel()
        mutableState.value = InboxState(isLoading = false)
    }
}
