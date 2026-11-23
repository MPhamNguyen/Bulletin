package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.application.MessageSenderPhotoProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Known inbox URLs start immediately; fallback metadata lookups run with bounded concurrency. */
internal suspend fun prefetchConversationAvatars(
    targets: List<ConversationAvatarPrefetchTarget>,
    photos: MessageSenderPhotoProvider,
    enqueue: (String) -> Unit
) = supervisorScope {
    val permits = Semaphore(3)
    targets.forEach { target ->
        launch {
            val url = target.url ?: permits.withPermit {
                try {
                    photos.photoUrl(target.senderId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
            }
            if (!url.isNullOrBlank()) enqueue(url)
        }
    }
}
