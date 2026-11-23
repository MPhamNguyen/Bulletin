package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.application.MessageSenderPhotoProvider
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PrefetchConversationAvatarsTest {
    @Test
    fun inboxUrlsStartWithoutMetadataLookupsOrWaitingForSlowPeers() = runTest {
        val gate = CompletableDeferred<Unit>()
        val requests = mutableListOf<SenderId>()
        val enqueued = mutableListOf<String>()
        val targets = listOf(
            ConversationAvatarPrefetchTarget(SenderId("slow"), null),
            ConversationAvatarPrefetchTarget(SenderId("known"), "ready.jpg"),
            ConversationAvatarPrefetchTarget(SenderId("fast"), null)
        )
        val job = launch {
            prefetchConversationAvatars(
                targets,
                MessageSenderPhotoProvider { id ->
                    requests += id
                    if (id.value == "slow") gate.await()
                    "${id.value}.jpg"
                },
                enqueued::add
            )
        }
        runCurrent()
        assertEquals(listOf("ready.jpg", "fast.jpg"), enqueued)
        assertEquals(listOf(SenderId("slow"), SenderId("fast")), requests)
        gate.complete(Unit)
        job.join()
        assertEquals("slow.jpg", enqueued.last())
    }

    @Test
    fun fallbackRequestsAreLimitedToThreeAndOneFailureDoesNotStopOtherImages() = runTest {
        val gate = CompletableDeferred<Unit>()
        var active = 0
        var maximum = 0
        val enqueued = mutableListOf<String>()
        val job = launch {
            prefetchConversationAvatars(
                (0..5).map {
                    ConversationAvatarPrefetchTarget(SenderId("$it"), null)
                },
                MessageSenderPhotoProvider { id ->
                    active++
                    maximum = maxOf(maximum, active)
                    gate.await()
                    active--
                    if (id.value == "0") error("unavailable")
                    "${id.value}.jpg"
                },
                enqueued::add
            )
        }
        runCurrent()
        assertEquals(3, active)
        gate.complete(Unit)
        job.join()
        assertTrue(maximum <= 3)
        assertEquals(5, enqueued.size)
    }
}
