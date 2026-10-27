package com.jdrms.bulletin.domain.listings.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.CreateListingErrorMessages
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingDto
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingInsertDto
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingWriteDto
import com.jdrms.bulletin.domain.listings.infrastructure.mapper.SupabaseListingMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage

class SupabaseListingsRepository internal constructor(
    private val listingsTable: SupabaseListingsTable,
    private val afterSuccessfulMutation: suspend () -> Unit,
    private val imageUrl: (String) -> String,
    private val deleteImages: suspend (String, String) -> Result<Unit>
) : ListingsRepository {
    internal constructor(listingsTable: SupabaseListingsTable) : this(
        listingsTable,
        {},
        { it },
        { _, _ -> Result.Success(Unit) }
    )

    internal constructor(
        listingsTable: SupabaseListingsTable,
        afterSuccessfulMutation: suspend () -> Unit
    ) : this(listingsTable, afterSuccessfulMutation, { it }, { _, _ -> Result.Success(Unit) })

    constructor(supabase: SupabaseClient) : this(
        PostgrestSupabaseListingsTable(supabase),
        {},
        { path -> supabase.storage.from(LISTING_IMAGE_BUCKET).publicUrl(path) },
        { listingId, sellerId ->
            SupabaseListingImagesRepository(supabase).deleteAll(ListingId(listingId), SellerId(sellerId))
        }
    )

    internal constructor(supabase: SupabaseClient, afterSuccessfulMutation: suspend () -> Unit) :
        this(
            PostgrestSupabaseListingsTable(supabase),
            afterSuccessfulMutation,
            { path -> supabase.storage.from(LISTING_IMAGE_BUCKET).publicUrl(path) },
            { listingId, sellerId ->
                SupabaseListingImagesRepository(supabase).deleteAll(ListingId(listingId), SellerId(sellerId))
            }
        )

    override suspend fun createListing(listing: Listing): Result<Listing> {
        return runCatching {
            if (listingsTable.findById(listing.id.value) != null) {
                Result.Error(IllegalStateException(CreateListingErrorMessages.GENERIC_FAILURE))
            } else {
                listingsTable.insert(SupabaseListingMapper.toInsertDto(listing))
                Result.Success(listing)
            }
        }.fold(
            onSuccess = { it },
            onFailure = { error ->
                Result.Error(Exception(CreateListingErrorMessages.GENERIC_FAILURE, error))
            }
        ).also { result -> if (result is Result.Success) afterSuccessfulMutation() }
    }

    override suspend fun updateListing(listing: Listing): Result<Listing> {
        return runCatching {
            val existing = listingsTable.findById(listing.id.value)
            if (existing == null) {
                Result.Error(NoSuchElementException("Listing not found with ID: ${listing.id.value}"))
            } else {
                listingsTable.update(SupabaseListingMapper.toInsertDto(listing))
                Result.Success(listing)
            }
        }.fold(
            onSuccess = { it },
            onFailure = { error ->
                Result.Error(Exception(CreateListingErrorMessages.GENERIC_FAILURE, error))
            }
        ).also { result -> if (result is Result.Success) afterSuccessfulMutation() }
    }

    override suspend fun getListing(id: ListingId): Listing? {
        return listingsTable.findById(id.value)?.let { SupabaseListingMapper.toDomain(it, imageUrl) }
    }

    override suspend fun deleteListing(id: ListingId, sellerId: SellerId): Result<Unit> {
        return runCatching {
            val existing = listingsTable.findById(id.value)
            if (existing == null) {
                Result.Error(NoSuchElementException("Listing not found."))
            } else {
                if (existing.userId != sellerId.value) {
                    Result.Error(IllegalAccessException("Only the owner can delete this listing."))
                } else {
                    when (val cleanup = deleteImages(id.value, sellerId.value)) {
                        is Result.Error -> cleanup
                        is Result.Success -> {
                            listingsTable.delete(id.value, sellerId.value)
                            Result.Success(Unit)
                        }
                    }
                }
            }
        }.fold(
            onSuccess = { it },
            onFailure = { error ->
                Result.Error(Exception(CreateListingErrorMessages.GENERIC_FAILURE, error))
            }
        ).also { result -> if (result is Result.Success) afterSuccessfulMutation() }
    }

    override suspend fun getSellerListings(sellerId: SellerId): List<Listing> {
        return listingsTable.getListings(sellerId.value).mapNotNull { SupabaseListingMapper.toDomain(it, imageUrl) }
    }

    override suspend fun getAllListings(): List<Listing> {
        return listingsTable.getListings(null).mapNotNull { SupabaseListingMapper.toDomain(it, imageUrl) }
    }

    companion object {
        const val LISTINGS_TABLE = "listings"
        const val LISTING_IMAGE_BUCKET = "listing_imgs"
    }
}

internal interface SupabaseListingsTable {
    suspend fun findById(id: String): SupabaseListingDto?
    suspend fun insert(listing: SupabaseListingInsertDto)
    suspend fun update(listing: SupabaseListingWriteDto)
    suspend fun delete(id: String, sellerId: String)
    suspend fun getListings(sellerId: String?): List<SupabaseListingDto>
}

private class PostgrestSupabaseListingsTable(
    private val supabase: SupabaseClient
) : SupabaseListingsTable {
    override suspend fun findById(id: String): SupabaseListingDto? {
        return supabase.from(SupabaseListingsRepository.LISTINGS_TABLE).select(columns = LISTING_COLUMNS) {
            filter { eq("id", id) }
        }.decodeSingleOrNull()
    }

    override suspend fun insert(listing: SupabaseListingInsertDto) {
        supabase.from(SupabaseListingsRepository.LISTINGS_TABLE).insert(listing)
    }

    override suspend fun update(listing: SupabaseListingWriteDto) {
        supabase.from(SupabaseListingsRepository.LISTINGS_TABLE).update(listing) {
            filter { eq("id", listing.id) }
        }
    }

    override suspend fun delete(id: String, sellerId: String) {
        supabase.from(SupabaseListingsRepository.LISTINGS_TABLE).delete {
            filter {
                eq("id", id)
                eq("user_id", sellerId)
            }
        }
    }

    override suspend fun getListings(sellerId: String?): List<SupabaseListingDto> {
        return supabase.from(SupabaseListingsRepository.LISTINGS_TABLE).select(columns = LISTING_COLUMNS) {
            sellerId?.let { filter { eq("user_id", it) } }
        }.decodeList()
    }

    private companion object {
        val LISTING_COLUMNS = Columns.raw(
            """
            *,
            seller_profile:profiles!listings_user_id_fkey ( full_name ),
            listing_images(object_path,position,crop_scale,crop_offset_x,crop_offset_y)
            """.trimIndent()
        )
    }
}
