package com.jdrms.bulletin.domain.marketplace

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceBlockedUserProvider
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MarketplaceBlockVisibilityTest {
    @Test
    fun searchOmitsListingsFromUsersHiddenFromViewer() = runTest {
        val search = SearchMarketplace(
            repository = InMemoryMarketplaceRepository(),
            blockedUserProvider = MarketplaceBlockedUserProvider { viewerId ->
                if (viewerId == "viewer") setOf("seller_101") else emptySet()
            }
        )

        val page = search.getPage(MarketplacePageRequest(viewerId = "viewer"))

        assertEquals(false, page.items.any { it.sellerId == "seller_101" })
    }

    @Test
    fun directListingViewReturnsUnavailableForHiddenSeller() = runTest {
        val view = ViewMarketplaceListing(
            repository = InMemoryMarketplaceRepository(),
            blockedUserProvider = MarketplaceBlockedUserProvider { setOf("seller_101") }
        )

        val result = view("mkt_1", viewerId = "viewer")

        assertEquals("This listing is unavailable.", assertIs<Result.Error>(result).exception.message)
    }
}
