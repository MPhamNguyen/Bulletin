package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.domain.messages.domain.repository.MessageStreamEvent
import com.jdrms.bulletin.domain.messages.infrastructure.repository.toMessageStreamEvent
import io.github.jan.supabase.realtime.RealtimeChannel
import kotlin.test.Test
import kotlin.test.assertEquals

class SupabaseRealtimeStatusMappingTest {
    @Test
    fun subscribingStatusIsReconnectingRatherThanFailed() {
        assertEquals(
            MessageStreamEvent.Reconnecting,
            RealtimeChannel.Status.SUBSCRIBING.toMessageStreamEvent()
        )
    }

    @Test
    fun disconnectedStatusesDoNotBecomeFailures() {
        assertEquals(
            MessageStreamEvent.Disconnected,
            RealtimeChannel.Status.UNSUBSCRIBED.toMessageStreamEvent()
        )
        assertEquals(
            MessageStreamEvent.Disconnected,
            RealtimeChannel.Status.UNSUBSCRIBING.toMessageStreamEvent()
        )
    }

    @Test
    fun subscribedStatusBecomesConnected() {
        assertEquals(
            MessageStreamEvent.Connected,
            RealtimeChannel.Status.SUBSCRIBED.toMessageStreamEvent()
        )
    }
}
