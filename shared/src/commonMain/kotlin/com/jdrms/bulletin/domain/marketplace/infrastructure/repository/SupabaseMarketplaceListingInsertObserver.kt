package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingInsertObserver
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterestOwnerId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class SupabaseMarketplaceListingInsertObserver(
    private val supabase: SupabaseClient
) : MarketplaceListingInsertObserver {
    override fun observe(ownerId: MarketplaceInterestOwnerId): Flow<MarketplaceItemId> = channelFlow {
        repeat(MAX_SUBSCRIBE_ATTEMPTS) { attempt ->
            trace("observer_attempt_${attempt + 1}")
            val failure = runCatching { observeChannel(ownerId) }.exceptionOrNull()
                ?: IllegalStateException("Listing interest stream ended unexpectedly")
            if (failure is CancellationException) throw failure
            trace("observer_failed_${attempt + 1}_${failure::class.simpleName}")
            if (attempt == MAX_SUBSCRIBE_ATTEMPTS - 1) return@channelFlow
            delay(RETRY_BASE_DELAY_MILLIS * (1L shl attempt))
        }
    }

    private suspend fun ProducerScope<MarketplaceItemId>.observeChannel(ownerId: MarketplaceInterestOwnerId) {
        val topic = topicFor(ownerId)
        val channel = supabase.channel(topic) {
            isPrivate = true
        }
        val inserts = channel.broadcastFlow<JsonObject>(BROADCAST_EVENT).mapNotNull { payload ->
            payload[LISTING_ID_KEY]?.jsonPrimitive?.content
                ?.takeIf(String::isNotBlank)?.let(::MarketplaceItemId)
                ?.also { trace("broadcast_insert_received") }
        }
        val connection = channel.status.drop(1).mapNotNull(::channelEndEvent)
        try {
            coroutineScope {
                val events = launch(start = CoroutineStart.UNDISPATCHED) {
                    merge(inserts.map { ListingChannelEvent.Inserted(it) }, connection)
                        .takeWhile { it is ListingChannelEvent.Inserted }
                        .collect { event -> send((event as ListingChannelEvent.Inserted).listingId) }
                }
                channel.subscribe(blockUntilSubscribed = true)
                trace("observer_subscribed")
                events.join()
            }
        } finally {
            withContext(NonCancellable) { runCatching { supabase.realtime.removeChannel(channel) } }
        }
    }
}

private fun trace(stage: String) {
    println("interest_notifications stage=$stage")
}

internal fun topicFor(ownerId: MarketplaceInterestOwnerId) = "$TOPIC_PREFIX${ownerId.value}"

private fun channelEndEvent(status: RealtimeChannel.Status): ListingChannelEvent? = when (status) {
    RealtimeChannel.Status.UNSUBSCRIBED,
    RealtimeChannel.Status.UNSUBSCRIBING -> ListingChannelEvent.Ended
    RealtimeChannel.Status.SUBSCRIBED,
    RealtimeChannel.Status.SUBSCRIBING -> null
}

private sealed interface ListingChannelEvent {
    data class Inserted(val listingId: MarketplaceItemId) : ListingChannelEvent
    data object Ended : ListingChannelEvent
}

private const val TOPIC_PREFIX = "marketplace-interests:"
private const val BROADCAST_EVENT = "listing-inserted"
private const val LISTING_ID_KEY = "listing_id"
private const val MAX_SUBSCRIBE_ATTEMPTS = 4
private const val RETRY_BASE_DELAY_MILLIS = 250L
