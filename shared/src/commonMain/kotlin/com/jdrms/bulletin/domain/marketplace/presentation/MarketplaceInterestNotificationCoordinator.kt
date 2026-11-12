package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceInterestAlert
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceInterestIdentity
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceInterestNotifier
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingInsertObserver
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterestOwnerId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal class MarketplaceInterestNotificationCoordinator(
    private val identity: MarketplaceInterestIdentity,
    private val listingObserver: MarketplaceListingInsertObserver,
    private val listingEvents: MarketplaceInterestListingEvents,
    private val matchLoader: MarketplaceInterestMatchLoader,
    private val notifier: MarketplaceInterestNotifier,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) {
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        trace("coordinator_started")
        job = scope.launch {
            identity.observeOwnerIds().distinctUntilChanged().collectLatest { owner ->
                trace(if (owner == null) "session_signed_out" else "session_authenticated")
                notifier.clearAll()
                if (owner == null) return@collectLatest
                trace("subscription_starting")
                listingObserver.observe(owner).collect { listingId ->
                    trace("listing_event_received")
                    listingEvents.publish(MarketplaceInterestListingEvent(owner, listingId))
                    notifyIfMatched(owner, listingId)
                }
            }
        }
    }

    private suspend fun notifyIfMatched(
        owner: MarketplaceInterestOwnerId,
        listingId: MarketplaceItemId
    ) {
        try {
            val listing = loadMatchingListing(listingId)
            trace("notifier_calling${if (listing != null) "_with_details" else "_generic"}")
            notifier.show(
                MarketplaceInterestAlert(
                    listingId = listingId,
                    recipientId = owner,
                    title = listing?.title ?: "New listing matches your interests",
                    summary = listing?.let { "${it.category.displayName()} - ${it.price.formatted}" }
                        ?: "Check the Marketplace for details",
                    createdAtMillis = listing?.createdAtMillis ?: System.currentTimeMillis()
                )
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Realtime notifications are best effort; the Interests page remains the source of truth.
        }
    }

    private suspend fun loadMatchingListing(listingId: MarketplaceItemId): MarketplaceItem? {
        repeat(MATCH_LOAD_ATTEMPTS) { attempt ->
            val listing = try {
                matchLoader.getById(listingId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                trace("match_load_failed_${attempt + 1}")
                null
            }
            trace(if (listing == null) "match_missing_${attempt + 1}" else "match_found_${attempt + 1}")
            if (listing != null) return listing
            if (attempt < MATCH_LOAD_ATTEMPTS - 1) {
                delay(MATCH_RETRY_DELAY_MILLIS)
                matchLoader.invalidate()
            }
        }
        return null
    }

    private fun trace(stage: String) {
        println("interest_notifications stage=$stage")
    }
}

private const val MATCH_LOAD_ATTEMPTS = 4
private const val MATCH_RETRY_DELAY_MILLIS = 500L
