package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import kotlinx.coroutines.CompletableDeferred

/** Controls send response order without delays or network calls. */
internal class PendingMessageSends {
    val requests = mutableListOf<Message>()
    private val completions = mutableListOf<CompletableDeferred<Result<Message>>>()
    private val backing = draftRepository()
    val repository = object : MessagesRepository by backing {
        override suspend fun sendMessage(message: Message): Result<Message> {
            val completion = CompletableDeferred<Result<Message>>()
            requests += message
            completions += completion
            return completion.await()
        }
    }

    fun succeed(index: Int) {
        completions[index].complete(Result.Success(requests[index]))
    }

    fun fail(index: Int, exception: Exception = IllegalStateException("Private transport detail")) {
        completions[index].complete(Result.Error(exception))
    }
}
