package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import kotlinx.coroutines.CoroutineDispatcher

internal class MessageSendingRecoveryFixture(dispatcher: CoroutineDispatcher) {
    private val backing = draftRepository()
    var inboxReads = 0
        private set
    var sendCount = 0
        private set
    var refreshFails = false
    private val repository = object : MessagesRepository by backing {
        override suspend fun getConversations(userId: SenderId): Result<List<Conversation>> {
            inboxReads++
            return if (refreshFails) historyFailure() else backing.getConversations(userId)
        }

        override suspend fun sendMessage(message: Message): Result<Message> {
            sendCount++
            // Simulate a stale inbox membership snapshot that cannot accept the delivered message.
            return Result.Success(message.copy(senderId = bob.id))
        }
    }
    val viewModel = draftViewModel(repository, MutableMessageSenderProvider(), dispatcher)
}
