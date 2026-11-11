package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingInsertObserver
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

class SupabaseMarketplaceListingInsertObserver(
    private val supabase: SupabaseClient
) : MarketplaceListingInsertObserver {
    override fun observe(): Flow<Unit> = channelFlow {
        val channel = supabase.channel("marketplace-interest-listings")
        val inserts = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "listings"
        }
        try {
            channel.subscribe(blockUntilSubscribed = true)
            inserts.collect { send(Unit) }
        } finally {
            withContext(NonCancellable) { runCatching { supabase.realtime.removeChannel(channel) } }
        }
    }
}
