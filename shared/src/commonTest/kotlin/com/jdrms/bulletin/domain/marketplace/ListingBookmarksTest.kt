package com.jdrms.bulletin.domain.marketplace

import com.jdrms.bulletin.domain.marketplace.domain.model.ListingBookmarks
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ListingBookmarksTest {
    @Test
    fun bookmarkIsUniquePerOwnerAndCanBeRemoved() {
        val listingId = MarketplaceItemId("listing:one")
        val bookmarks = ListingBookmarks.of("user-one").bookmark(listingId).bookmark(listingId)

        assertEquals(setOf(listingId), bookmarks.ids)
        assertEquals(emptySet(), bookmarks.remove(listingId).ids)
        assertEquals(emptySet(), ListingBookmarks.of("user-two").ids)
    }

    @Test
    fun rejectsBlankOwnerAndListingId() {
        assertFailsWith<IllegalArgumentException> { ListingBookmarks.of(" ") }
        assertFailsWith<IllegalArgumentException> {
            ListingBookmarks.of("user-one", listOf(MarketplaceItemId(" ")))
        }
    }
}
