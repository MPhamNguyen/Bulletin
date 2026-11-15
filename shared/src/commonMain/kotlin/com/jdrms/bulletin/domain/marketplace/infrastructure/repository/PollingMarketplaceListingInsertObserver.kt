package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingInsertObserver
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterestOwnerId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withTimeoutOrNull

internal class PollingMarketplaceListingInsertObserver(
    private val realtime: MarketplaceListingInsertObserver,
    private val snapshots: MarketplaceListingIdSnapshotSource,
    private val pollingIntervalMillis: Long = DEFAULT_POLLING_INTERVAL_MILLIS
) : MarketplaceListingInsertObserver {
    override fun observe(ownerId: MarketplaceInterestOwnerId): Flow<MarketplaceItemId> = flow {
        val delivered = mutableSetOf<MarketplaceItemId>()
        merge(realtime.observe(ownerId), pollForInserts()).collect { listingId ->
            if (delivered.add(listingId)) emit(listingId)
        }
    }

    private fun pollForInserts(): Flow<MarketplaceItemId> = flow {
        var knownIds: Set<MarketplaceItemId>? = null
        while (true) {
            val latestIds = runCatching {
                withTimeoutOrNull(SNAPSHOT_TIMEOUT_MILLIS) { snapshots.latestIds() }.also { snapshot ->
                    trace(snapshot?.let { "snapshot_loaded_${it.size}" } ?: "snapshot_timed_out")
                }
            }.getOrElse { failure ->
                if (failure is CancellationException) throw failure
                trace("snapshot_failed_${failure::class.simpleName}")
                null
            }
            if (latestIds != null) {
                knownIds?.let { known ->
                    latestIds.filterNot(known::contains).also { newIds ->
                        trace("snapshot_new_${newIds.size}")
                    }.forEach { emit(it) }
                }
                if (knownIds == null) trace("snapshot_baseline_ready")
                knownIds = knownIds.orEmpty() + latestIds
            }
            delay(pollingIntervalMillis)
        }
    }
}

private const val DEFAULT_POLLING_INTERVAL_MILLIS = 10_000L
private const val SNAPSHOT_TIMEOUT_MILLIS = 5_000L

private fun trace(stage: String) {
    println("interest_notifications stage=$stage")
}
