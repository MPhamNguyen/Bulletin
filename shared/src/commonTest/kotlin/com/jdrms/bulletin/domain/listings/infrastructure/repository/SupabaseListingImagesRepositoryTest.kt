package com.jdrms.bulletin.domain.listings.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.ImageCropSettings
import com.jdrms.bulletin.core.common.withCropSettings
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
            listOf(
                "https://cdn/listing/a.jpeg#bulletin_crop=1.0,0.0,0.0",
                "https://cdn/listing/b.jpeg#bulletin_crop=1.0,0.0,0.0"
            ),
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

    @Test
    fun failedMetadataRemovalKeepsExistingStoredImages() = runTest {
        val oldImage = ListingImageRow("listing/old.jpeg", 0)
        val storage = FakeListingImageStorage()
        val rows = FakeListingImageDataStore(
            initial = listOf(oldImage),
            failOnRemove = true
        )
        val repository = SupabaseListingImagesRepository(
            rows = rows,
            storage = storage
        )

        val result = repository.synchronize(
            ListingId("listing"),
            SellerId("seller"),
            emptyList(),
            listOf(image())
        )

        assertTrue(result is Result.Error)
        assertTrue(storage.deleted.none { oldImage.objectPath in it })
        assertEquals(listOf(oldImage), rows.get("listing"))
    }

    @Test
    fun deleteAllRemovesMetadataBeforeStoredImages() = runTest {
        val oldImage = ListingImageRow("listing/old.jpeg", 0)
        val storage = FakeListingImageStorage()
        val rows = FakeListingImageDataStore(initial = listOf(oldImage))
        val repository = SupabaseListingImagesRepository(rows = rows, storage = storage)

        val result = repository.deleteAll(ListingId("listing"), SellerId("seller"))

        assertTrue(result is Result.Success)
        assertEquals(listOf(listOf(oldImage.objectPath)), storage.deleted)
        assertTrue(rows.get("listing").isEmpty())
    }

    @Test
    fun removingFirstImageAndAddingOneKeepsDraftOrder() = runTest {
        val first = ListingImageRow("listing/first.jpeg", 0)
        val second = ListingImageRow("listing/second.jpeg", 1)
        val storage = FakeListingImageStorage()
        val rows = FakeListingImageDataStore(initial = listOf(first, second))
        val repository = SupabaseListingImagesRepository(
            rows = rows,
            storage = storage,
            createPath = { "third" }
        )

        val result = repository.synchronize(
            ListingId("listing"),
            SellerId("seller"),
            listOf(storage.publicUrl(second.objectPath)),
            listOf(image())
        )

        assertTrue(result is Result.Success)
        assertEquals(
            listOf(
                "https://cdn/listing/second.jpeg#bulletin_crop=1.0,0.0,0.0",
                "https://cdn/listing/third.jpeg#bulletin_crop=1.0,0.0,0.0"
            ),
            result.data
        )
        assertEquals(
            listOf(
                ListingImageRow(second.objectPath, 0),
                ListingImageRow("listing/third.jpeg", 1)
            ),
            rows.get("listing").sortedBy(ListingImageRow::position)
        )
    }

    @Test
    fun persistsCropSettingsWithUploadedImageMetadata() = runTest {
        val rows = FakeListingImageDataStore()
        val repository = SupabaseListingImagesRepository(
            rows = rows,
            storage = FakeListingImageStorage(),
            createPath = { "cropped" }
        )

        val result = repository.synchronize(
            ListingId("listing"),
            SellerId("seller"),
            emptyList(),
            listOf(ListingImageUpload(byteArrayOf(1), "image/jpeg", 2f, 0.2f, -0.1f))
        )

        assertTrue(result is Result.Success)
        assertEquals(
            "https://cdn/listing/cropped.jpeg".withCropSettings(ImageCropSettings(2f, 0.2f, -0.1f)),
            result.data.single()
        )
        assertEquals(ListingImageRow("listing/cropped.jpeg", 0, 2f, 0.2f, -0.1f), rows.get("listing").single())
    }

    private fun image() = ListingImageUpload(byteArrayOf(1), "image/jpeg")
}

private class FakeListingImageDataStore(
    initial: List<ListingImageRow> = emptyList(),
    private val failOnRemove: Boolean = false
) : ListingImageDataStore {
    private val images = initial.toMutableList()
    override suspend fun get(listingId: String): List<ListingImageRow> = images.toList()
    override suspend fun insert(listingId: String, rows: List<ListingImageRow>) { images += rows }
    override suspend fun updatePosition(listingId: String, objectPath: String, position: Int) {
        val index = images.indexOfFirst { it.objectPath == objectPath }
        if (index >= 0) images[index] = images[index].copy(position = position)
    }
    override suspend fun updateCrop(listingId: String, objectPath: String, crop: ImageCropSettings) {
        val index = images.indexOfFirst { it.objectPath == objectPath }
        if (index >= 0) images[index] = images[index].copy(
            cropScale = crop.scale, cropOffsetX = crop.offsetX, cropOffsetY = crop.offsetY
        )
    }
    override suspend fun remove(listingId: String, objectPaths: List<String>) {
        if (failOnRemove) error("Metadata removal failed")
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
