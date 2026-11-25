package com.jdrms.bulletin.domain.listings.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.CreateListingErrorMessages
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingUpdateIntent
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.listings.infrastructure.mapper.SupabaseListingMapper
import io.github.jan.supabase.SupabaseClient
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

    override suspend fun updateListing(listing: Listing, intent: ListingUpdateIntent): Result<Listing> {
        var stage = ListingWriteStage.READ_BEFORE_WRITE
        return runCatching {
            val existing = listingsTable.findById(listing.id.value)
            if (existing == null) {
                Result.Error(NoSuchElementException("Listing not found with ID: ${listing.id.value}"))
            } else {
                stage = if (intent == ListingUpdateIntent.EDIT) {
                    ListingWriteStage.EDIT_RPC
                } else {
                    ListingWriteStage.ROUTINE_UPDATE
                }
                listingsTable.update(SupabaseListingMapper.toInsertDto(listing), intent)
                stage = ListingWriteStage.READ_AFTER_WRITE
                Result.Success(
                    requireNotNull(getListing(listing.id)) { "Saved listing could not be read." }
                )
            }
        }.fold(
            onSuccess = { it },
            onFailure = { error ->
                reportListingWriteFailure(stage, error)
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
