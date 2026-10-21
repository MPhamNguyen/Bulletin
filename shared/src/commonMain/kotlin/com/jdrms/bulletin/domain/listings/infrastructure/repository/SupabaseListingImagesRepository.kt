package com.jdrms.bulletin.domain.listings.infrastructure.repository

import com.jdrms.bulletin.core.common.ImageCropSettings
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.cropSettings
import com.jdrms.bulletin.core.common.generateUuid
import com.jdrms.bulletin.core.common.withCropSettings
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.repository.ListingImagesRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ListingImageRow(
    @SerialName("object_path") val objectPath: String,
    @SerialName("position") val position: Int,
    @SerialName("crop_scale") val cropScale: Float = 1f,
    @SerialName("crop_offset_x") val cropOffsetX: Float = 0f,
    @SerialName("crop_offset_y") val cropOffsetY: Float = 0f
)

@Serializable
private data class ListingImagePositionDto(@SerialName("position") val position: Int)

@Serializable
private data class ListingImageCropDto(
    @SerialName("crop_scale") val scale: Float,
    @SerialName("crop_offset_x") val offsetX: Float,
    @SerialName("crop_offset_y") val offsetY: Float
)

@Serializable
private data class ListingImageInsertDto(
    @SerialName("listing_id") val listingId: String,
    @SerialName("object_path") val objectPath: String,
    @SerialName("position") val position: Int,
    @SerialName("crop_scale") val cropScale: Float = 1f,
    @SerialName("crop_offset_x") val cropOffsetX: Float = 0f,
    @SerialName("crop_offset_y") val cropOffsetY: Float = 0f
)

internal interface ListingImageDataStore {
    suspend fun get(listingId: String): List<ListingImageRow>
    suspend fun insert(listingId: String, rows: List<ListingImageRow>)
    suspend fun updatePosition(listingId: String, objectPath: String, position: Int)
    suspend fun updateCrop(listingId: String, objectPath: String, crop: ImageCropSettings)
    suspend fun remove(listingId: String, objectPaths: List<String>)
}

internal interface ListingImageStorage {
    suspend fun upload(path: String, image: ListingImageUpload)
    suspend fun delete(paths: List<String>)
    fun publicUrl(path: String): String
    fun authenticatedUserId(): String?
}

class SupabaseListingImagesRepository internal constructor(
    private val rows: ListingImageDataStore,
    private val storage: ListingImageStorage,
    private val createPath: () -> String = ::generateUuid
) : ListingImagesRepository {
    constructor(supabase: SupabaseClient) : this(
        PostgrestListingImageDataStore(supabase),
        SupabaseListingImageStorage(supabase)
    )

    override suspend fun synchronize(
        listingId: ListingId,
        sellerId: SellerId,
        retainedImageUrls: List<String>,
        newImages: List<ListingImageUpload>
    ): Result<List<String>> = guardedResult {
        requireOwner(sellerId)
        val current = rows.get(listingId.value).sortedBy(ListingImageRow::position)
        val retainedPaths = resolveRetainedImages(current, retainedImageUrls, newImages.size)

        val retainedSet = retainedPaths.map(ListingImageRow::objectPath).toSet()
        val removed = current.filterNot { it.objectPath in retainedSet }
        val orderedRetained = retainedPaths.mapIndexed { index, row ->
            val crop = retainedImageUrls[index].cropSettings()
            row.copy(
                position = index,
                cropScale = crop.scale,
                cropOffsetX = crop.offsetX,
                cropOffsetY = crop.offsetY
            )
        }
        val uploaded = uploadNewImages(
            listingId,
            newImages,
            (retainedPaths.size until retainedPaths.size + newImages.size).toList()
        )
        persistImageChanges(listingId, removed, retainedPaths, orderedRetained, uploaded)
        if (removed.isNotEmpty()) storage.delete(removed.map(ListingImageRow::objectPath))
        (orderedRetained + uploaded).map { row ->
            storage.publicUrl(row.objectPath).withCropSettings(
                ImageCropSettings(row.cropScale, row.cropOffsetX, row.cropOffsetY)
            )
        }
    }

    private fun resolveRetainedImages(
        current: List<ListingImageRow>,
        retainedImageUrls: List<String>,
        newImageCount: Int
    ): List<ListingImageRow> {
        val currentByUrl = current.associateBy { row -> row.toCropUrl() }
        val retained = retainedImageUrls.mapNotNull { url ->
            currentByUrl[url] ?: current.firstOrNull {
                storage.publicUrl(it.objectPath) == url.substringBefore("#bulletin_crop=")
            }
        }
        val uniqueRetainedPaths = retained.map(ListingImageRow::objectPath).distinct()
        val uniqueRequestedPaths = retainedImageUrls.map { it.substringBefore("#bulletin_crop=") }.distinct()
        require(retained.size == uniqueRetainedPaths.size && uniqueRetainedPaths.size == uniqueRequestedPaths.size) {
            "One or more selected listing photos are no longer available. Refresh the listing and try again."
        }
        require(retained.size + newImageCount <= ListingImageUpload.MAX_LISTING_IMAGE_COUNT) {
            "A listing can have up to 5 images."
        }
        return retained
    }

    private fun ListingImageRow.toCropUrl(): String = storage.publicUrl(objectPath).withCropSettings(
        ImageCropSettings(cropScale, cropOffsetX, cropOffsetY)
    )

    private suspend fun persistImageChanges(
        listingId: ListingId,
        removed: List<ListingImageRow>,
        previousRetained: List<ListingImageRow>,
        updatedRetained: List<ListingImageRow>,
        uploaded: List<ListingImageRow>
    ) {
        val persistence = runCatching {
            if (removed.isNotEmpty()) rows.remove(listingId.value, removed.map(ListingImageRow::objectPath))
            previousRetained.forEachIndexed { index, oldRow ->
                val updated = updatedRetained[index]
                if (oldRow.position != index) rows.updatePosition(listingId.value, oldRow.objectPath, index)
                if (oldRow.cropScale != updated.cropScale || oldRow.cropOffsetX != updated.cropOffsetX ||
                    oldRow.cropOffsetY != updated.cropOffsetY
                ) {
                    rows.updateCrop(
                        listingId.value,
                        oldRow.objectPath,
                        ImageCropSettings(updated.cropScale, updated.cropOffsetX, updated.cropOffsetY)
                    )
                }
            }
            rows.insert(listingId.value, uploaded)
        }
        persistence.onFailure { cleanupUploads(uploaded) }.getOrElse { throw it }
    }

    override suspend fun deleteAll(listingId: ListingId, sellerId: SellerId): Result<Unit> = guardedResult {
        requireOwner(sellerId)
        val existing = rows.get(listingId.value)
        if (existing.isNotEmpty()) {
            rows.remove(listingId.value, existing.map(ListingImageRow::objectPath))
            storage.delete(existing.map(ListingImageRow::objectPath))
        }
    }

    private suspend fun uploadNewImages(
        listingId: ListingId,
        images: List<ListingImageUpload>,
        positions: List<Int>
    ): List<ListingImageRow> {
        val uploaded = mutableListOf<ListingImageRow>()
        val uploadResult = runCatching {
            images.forEachIndexed { index, image ->
                val extension = image.mediaType.substringAfter('/')
                val row = ListingImageRow(
                    "${listingId.value}/${createPath()}.$extension",
                    positions[index],
                    image.cropScale,
                    image.cropOffsetX,
                    image.cropOffsetY
                )
                storage.upload(row.objectPath, image)
                uploaded += row
            }
            uploaded.toList()
        }
        return uploadResult.onFailure { cleanupUploads(uploaded) }
            .getOrElse { throw it }
    }

    private suspend fun cleanupUploads(images: List<ListingImageRow>) {
        if (images.isNotEmpty()) runCatching { storage.delete(images.map(ListingImageRow::objectPath)) }
    }

    private fun requireOwner(sellerId: SellerId) {
        require(storage.authenticatedUserId() == sellerId.value) {
            "You do not have permission to manage images for this listing."
        }
    }

    private suspend fun <T> guardedResult(block: suspend () -> T): Result<T> =
        runCatching { block() }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { error ->
                if (error is CancellationException) throw error
                Result.Error(error)
            }
        )

    private class PostgrestListingImageDataStore(private val supabase: SupabaseClient) : ListingImageDataStore {
        override suspend fun get(listingId: String): List<ListingImageRow> =
            supabase.from(TABLE).select(columns = IMAGE_COLUMNS) {
                filter { eq("listing_id", listingId) }
                order("position", Order.ASCENDING)
            }.decodeList()

        override suspend fun insert(listingId: String, rows: List<ListingImageRow>) {
            if (rows.isNotEmpty()) {
                supabase.from(TABLE).insert(
                    rows.map { row ->
                        ListingImageInsertDto(
                            listingId,
                            row.objectPath,
                            row.position,
                            row.cropScale,
                            row.cropOffsetX,
                            row.cropOffsetY
                        )
                    }
                )
            }
        }

        override suspend fun updatePosition(listingId: String, objectPath: String, position: Int) {
            supabase.from(TABLE).update(ListingImagePositionDto(position)) {
                filter {
                    eq("listing_id", listingId)
                    eq("object_path", objectPath)
                }
            }
        }

        override suspend fun updateCrop(listingId: String, objectPath: String, crop: ImageCropSettings) {
            supabase.from(TABLE).update(ListingImageCropDto(crop.scale, crop.offsetX, crop.offsetY)) {
                filter {
                    eq("listing_id", listingId)
                    eq("object_path", objectPath)
                }
            }
        }

        override suspend fun remove(listingId: String, objectPaths: List<String>) {
            objectPaths.forEach { path ->
                supabase.from(TABLE).delete {
                    filter {
                        eq("listing_id", listingId)
                        eq("object_path", path)
                    }
                }
            }
        }

        private companion object {
            const val TABLE = "listing_images"
            val IMAGE_COLUMNS = io.github.jan.supabase.postgrest.query.Columns.raw(
                "object_path,position,crop_scale,crop_offset_x,crop_offset_y"
            )
        }
    }

    private class SupabaseListingImageStorage(private val supabase: SupabaseClient) : ListingImageStorage {
        override suspend fun upload(path: String, image: ListingImageUpload) {
            supabase.storage.from(BUCKET).upload(path, image.bytes) {
                contentType = ContentType.parse(image.mediaType)
            }
        }

        override suspend fun delete(paths: List<String>) {
            if (paths.isNotEmpty()) supabase.storage.from(BUCKET).delete(paths)
        }

        override fun publicUrl(path: String): String = supabase.storage.from(BUCKET).publicUrl(path)
        override fun authenticatedUserId(): String? = supabase.auth.currentUserOrNull()?.id

        private companion object {
            const val BUCKET = "listing_imgs"
        }
    }

    private companion object {
        const val TABLE = "listing_images"
    }
}
