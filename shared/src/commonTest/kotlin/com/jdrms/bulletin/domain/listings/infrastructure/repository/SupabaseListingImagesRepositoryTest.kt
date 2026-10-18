package com.jdrms.bulletin.domain.listings.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SupabaseListingImagesRepositoryTest {
    @Test
    fun uploadsAndReturnsImagesInDisplayOrder() = runTest {
        val storage = FakeListingImageStorage()
        val repository = SupabaseListingImagesRepository(
            rows = FakeListingImageDataStore(),
            storage = storage,
            createPath = sequenceOf("a", "b").iterator()::next
        )

        val result = repository.synchronize(
            ListingId("listing"),
            SellerId("seller"),
            emptyList(),
            listOf(image(), image())
        )

        assertTrue(result is Result.Success)
        assertEquals(
            listOf("https://cdn/listing/a.jpeg", "https://cdn/listing/b.jpeg"),
            result.data
        )
        assertEquals(listOf("listing/a.jpeg", "listing/b.jpeg"), storage.uploaded)
    }

    @Test
    fun failedUploadCleansPreviouslyUploadedObjects() = runTest {
        val storage = FakeListingImageStorage(failOnUpload = 2)
        val repository = SupabaseListingImagesRepository(
            rows = FakeListingImageDataStore(),
            storage = storage,
            createPath = sequenceOf("a", "b").iterator()::next
        )

        val result = repository.synchronize(
            ListingId("listing"),
            SellerId("seller"),
            emptyList(),
            listOf(image(), image())
        )

        assertTrue(result is Result.Error)
        assertEquals(listOf("listing/a.jpeg"), storage.deleted.single())
        assertTrue(storage.uploaded.contains("listing/a.jpeg"))
    }

    private fun image() = ListingImageUpload(byteArrayOf(1), "image/jpeg")
}

private class FakeListingImageDataStore : ListingImageDataStore {
    private val images = mutableListOf<ListingImageRow>()
    override suspend fun get(listingId: String): List<ListingImageRow> = images.toList()
    override suspend fun insert(listingId: String, rows: List<ListingImageRow>) { images += rows }
    override suspend fun remove(listingId: String, objectPaths: List<String>) {
        images.removeAll { it.objectPath in objectPaths }
    }
}

private class FakeListingImageStorage(private val failOnUpload: Int? = null) : ListingImageStorage {
    val uploaded = mutableListOf<String>()
    val deleted = mutableListOf<List<String>>()
    override suspend fun upload(path: String, image: ListingImageUpload) {
        if (uploaded.size + 1 == failOnUpload) error("Upload failed")
        uploaded += path
    }
    override suspend fun delete(paths: List<String>) { deleted += paths }
    override fun publicUrl(path: String): String = "https://cdn/$path"
    override fun authenticatedUserId(): String = "seller"
}
