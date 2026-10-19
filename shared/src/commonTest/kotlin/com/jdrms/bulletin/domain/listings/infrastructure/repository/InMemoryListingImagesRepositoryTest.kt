package com.jdrms.bulletin.domain.listings.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class InMemoryListingImagesRepositoryTest {
    @Test
    fun addingAfterRetainingOneImageCreatesUniqueUrl() = runTest {
        val repository = InMemoryListingImagesRepository()
        val listingId = ListingId("listing")
        val sellerId = SellerId("seller")

        val initial = repository.synchronize(listingId, sellerId, emptyList(), listOf(image(), image()))
        assertTrue(initial is Result.Success)
        val retainedUrl = initial.data[1]

        val updated = repository.synchronize(listingId, sellerId, listOf(retainedUrl), listOf(image()))

        assertTrue(updated is Result.Success)
        assertEquals(2, updated.data.size)
        assertEquals(retainedUrl, updated.data.first())
        assertNotEquals(updated.data[0], updated.data[1])
    }

    private fun image() = ListingImageUpload(byteArrayOf(1), "image/jpeg")
}
