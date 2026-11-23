package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import com.jdrms.bulletin.domain.messages.application.ConversationListingPreview
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ConversationListingDetailPreviewTest {
    @Test
    fun conversationRetainsListingDataForOpeningDetails() = runTest {
        val listings = InMemoryListingsRepository()
        val listing = listings.getAllListings().first()
        val id = "listing:${listing.id.value}"
        val provider = MarketplaceConversationListingPreviewProvider(listings, InMemoryMarketplaceRepository())
        val card = assertNotNull(provider.preview(ListingReferenceId(id)))
        val preview = assertNotNull(card.toMarketplaceDetailPreview(id))
        assertEquals(id, preview.listingId)
        assertEquals(listing.sellerId.value, preview.sellerId)
        assertEquals(listing.sellerName, preview.sellerName)
        with(preview.content) {
            assertEquals(listing.title, title)
            assertEquals(listing.price.formatted, price)
            assertEquals(listing.description, description)
            assertEquals(listing.category.name, category)
            assertEquals(listing.condition.name, condition)
            assertEquals(listing.images, photos)
            assertEquals(listing.createdAtMillis, createdAtMillis)
            assertEquals(listing.updatedAtMillis, updatedAtMillis)
        }
    }

    @Test
    fun marketplaceFallbackAlsoRetainsDetailData() = runTest {
        val marketplace = InMemoryMarketplaceRepository()
        val item = assertNotNull(marketplace.getItem(MarketplaceItemId("mkt_1")))
        val provider =
            MarketplaceConversationListingPreviewProvider(InMemoryListingsRepository(emptyList()), marketplace)
        val card = assertNotNull(provider.preview(ListingReferenceId(item.id.value)))
        val preview = assertNotNull(card.toMarketplaceDetailPreview(item.id.value))
        assertEquals(item.toConversationPreview(), card)
        assertEquals(item.description, preview.content.description)
        assertEquals(item.photos, preview.content.photos)
        assertEquals(item.sellerId, preview.sellerId)
    }

    @Test
    fun cardWithoutDetailSnapshotUsesNormalDetailLoading() {
        assertNull(ConversationListingPreview("Desk", "$20.00", null).toMarketplaceDetailPreview("listing:1"))
    }
}
