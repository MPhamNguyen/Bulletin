package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.core.common.BoundedMemoryCache
import com.jdrms.bulletin.core.common.CacheRefreshResult
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingPage
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId

internal class MarketplaceReadCache(
    currentTimeMillis: () -> Long = ::marketplaceCacheTimeMillis
) {
    private val pages = BoundedMemoryCache<MarketplacePageRequest, MarketplaceListingPage>(
        MAX_ENTRIES,
        TTL_MILLIS,
        currentTimeMillis
    )
    private val catalog = BoundedMemoryCache<String, List<MarketplaceItem>>(MAX_ENTRIES, TTL_MILLIS, currentTimeMillis)
    private val items =
        BoundedMemoryCache<MarketplaceItemId, MarketplaceItem>(MAX_ENTRIES, TTL_MILLIS, currentTimeMillis)
    private val details = BoundedMemoryCache<String, Listing>(MAX_ENTRIES, TTL_MILLIS, currentTimeMillis)

    suspend fun page(request: MarketplacePageRequest, loader: suspend () -> MarketplaceListingPage) =
        pages.getOrLoad(request, loader)!!

    suspend fun refreshPage(request: MarketplacePageRequest, loader: suspend () -> MarketplaceListingPage):
        CacheRefreshResult<MarketplaceListingPage> =
        pages.refresh(request, loader)!!

    suspend fun catalog(loader: suspend () -> List<MarketplaceItem>) = catalog.getOrLoad(CATALOG_KEY, loader)!!

    suspend fun item(id: MarketplaceItemId, loader: suspend () -> MarketplaceItem?): MarketplaceItem? =
        loaderOrCached(id, loader)

    private suspend fun loaderOrCached(
        id: MarketplaceItemId,
        loader: suspend () -> MarketplaceItem?
    ): MarketplaceItem? = items.getOrLoad(id) { loader() ?: return@getOrLoad null }

    suspend fun detail(id: String, loader: suspend () -> Listing) = details.getOrLoad(id, loader)!!

    suspend fun invalidateAll() {
        pages.clear()
        catalog.clear()
        items.clear()
        details.clear()
    }

    companion object {
        const val TTL_MILLIS = 45_000L
        const val MAX_ENTRIES = 64
        private const val CATALOG_KEY = "catalog"
    }
}

private val marketplaceClockStart = kotlin.time.TimeSource.Monotonic.markNow()
private fun marketplaceCacheTimeMillis(): Long = marketplaceClockStart.elapsedNow().inWholeMilliseconds
