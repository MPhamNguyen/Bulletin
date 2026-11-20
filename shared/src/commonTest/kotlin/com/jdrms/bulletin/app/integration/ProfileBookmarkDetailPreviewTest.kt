package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.designsystem.ItemDetailRow
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListing
import com.jdrms.bulletin.domain.profile.application.ProfileListingAttribute
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileBookmarkDetailPreviewTest {
    @Test
    fun bookmarkDisplayDataSurvivesTheDetailBoundary() {
        val bookmark = ProfileBookmarkedListing(
            id = "listing:123", sellerId = "seller", sellerName = "Alex",
            title = "Desk", price = "$25.00", category = "FURNITURE",
            condition = "GOOD", description = "Wooden desk", photos = listOf("photo"),
            createdAtMillis = 123L, attributes = listOf(ProfileListingAttribute("Material", "Wood"))
        )
        val preview = bookmark.toMarketplaceDetailPreview()
        assertEquals(bookmark.id, preview.listingId)
        assertEquals(bookmark.sellerId, preview.sellerId)
        assertEquals(bookmark.sellerName, preview.sellerName)
        with(preview.content) {
            assertEquals(bookmark.title, title)
            assertEquals(bookmark.price, price)
            assertEquals(bookmark.category, category)
            assertEquals(bookmark.condition, condition)
            assertEquals(bookmark.description, description)
            assertEquals(bookmark.photos, photos)
            assertEquals(bookmark.createdAtMillis, createdAtMillis)
            assertEquals(listOf(ItemDetailRow("Material", "Wood")), attributes)
        }
    }
}
