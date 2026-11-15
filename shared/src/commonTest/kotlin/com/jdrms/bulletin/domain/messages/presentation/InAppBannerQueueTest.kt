package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InAppBannerQueueTest {
    private val queue = InAppBannerQueue()

    @Test
    fun `same conversation replaces the visible banner`() {
        queue.offer(bannerAlert("m1", "a"))
        queue.offer(bannerAlert("m2", "a"))
        assertEquals(MessageId("m2"), queue.head?.messageId)
        assertTrue(queue.dismiss(MessageId("m2")))
        assertNull(queue.head)
    }

    @Test
    fun `different conversation queues behind the visible banner`() {
        queue.offer(bannerAlert("m1", "a"))
        queue.offer(bannerAlert("m2", "b"))
        assertEquals(MessageId("m1"), queue.head?.messageId)
        queue.dismiss(MessageId("m1"))
        assertEquals(MessageId("m2"), queue.head?.messageId)
    }

    @Test
    fun `queue is capped at two and drops the oldest waiting banner`() {
        queue.offer(bannerAlert("m1", "a"))
        queue.offer(bannerAlert("m2", "b"))
        queue.offer(bannerAlert("m3", "c"))
        assertEquals(MessageId("m1"), queue.head?.messageId)
        queue.dismiss(MessageId("m1"))
        assertEquals(MessageId("m3"), queue.head?.messageId)
        queue.dismiss(MessageId("m3"))
        assertNull(queue.head)
    }

    @Test
    fun `replacing a waiting banner keeps its position and the cap`() {
        queue.offer(bannerAlert("m1", "a"))
        queue.offer(bannerAlert("m2", "b"))
        queue.offer(bannerAlert("m3", "b"))
        queue.dismiss(MessageId("m1"))
        assertEquals(MessageId("m3"), queue.head?.messageId)
    }

    @Test
    fun `dismissing a replaced message is a no-op`() {
        queue.offer(bannerAlert("m1", "a"))
        queue.offer(bannerAlert("m2", "a"))
        assertFalse(queue.dismiss(MessageId("m1")))
        assertEquals(MessageId("m2"), queue.head?.messageId)
    }

    @Test
    fun `clear empties the queue`() {
        queue.offer(bannerAlert("m1", "a"))
        queue.offer(bannerAlert("m2", "b"))
        queue.clear()
        assertNull(queue.head)
    }
}
