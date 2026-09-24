package com.jdrms.bulletin.domain.listings.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.CreateListingErrorMessages
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingDto
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingInsertDto
import com.jdrms.bulletin.domain.listings.infrastructure.mapper.SupabaseListingMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

class SupabaseListingsRepository internal constructor(
    private val listingsTable: SupabaseListingsTable
) : ListingsRepository {
    constructor(supabase: SupabaseClient) : this(PostgrestSupabaseListingsTable(supabase))

    override suspend fun createListing(listing: Listing): Result<Listing> {
        return runCatching {
            val sellerId = requireAuthenticatedSeller(listing.sellerId.value)
            if (listingsTable.findById(listing.id.value) != null) {
                Result.Error(IllegalStateException(CreateListingErrorMessages.GENERIC_FAILURE))
            } else {
                listingsTable.insert(SupabaseListingMapper.toInsertDto(listing, sellerId))
                Result.Success(listing.copy(sellerId = SellerId(sellerId)))
            }
        }.fold(
            onSuccess = { it },
            onFailure = { error ->
                Result.Error(Exception(CreateListingErrorMessages.GENERIC_FAILURE, error))
            }
        )
    }

    override suspend fun updateListing(listing: Listing): Result<Listing> {
        return Result.Error(UnsupportedOperationException("Listing updates are not implemented for Supabase."))
    }

    override suspend fun deleteListing(id: ListingId): Result<Unit> {
        return runCatching {
            val existing = listingsTable.findById(id.value)
            if (existing == null) {
                Result.Error(NoSuchElementException("Listing not found."))
            } else {
                requireAuthenticatedSeller(existing.userId.orEmpty())
                listingsTable.delete(id.value)
                Result.Success(Unit)
            }
        }.fold(
            onSuccess = { it },
            onFailure = { error ->
                Result.Error(Exception(CreateListingErrorMessages.GENERIC_FAILURE, error))
            }
        )
    }

    override suspend fun getSellerListings(sellerId: SellerId): List<Listing> {
        return listingsTable.getListings(sellerId.value).mapNotNull(SupabaseListingMapper::toDomain)
    }

    override suspend fun getAllListings(): List<Listing> {
        return listingsTable.getListings(null).mapNotNull(SupabaseListingMapper::toDomain)
    }

    private suspend fun requireAuthenticatedSeller(sellerId: String): String {
        val authenticatedUserId = listingsTable.authenticatedUserId()
            ?: error(CreateListingErrorMessages.AUTHENTICATION_REQUIRED)
        require(authenticatedUserId == sellerId) {
            CreateListingErrorMessages.GENERIC_FAILURE
        }
        return authenticatedUserId
    }

    companion object {
        const val LISTINGS_TABLE = "listings"
    }
}

internal interface SupabaseListingsTable {
    suspend fun findById(id: String): SupabaseListingDto?
    suspend fun insert(listing: SupabaseListingInsertDto)
    suspend fun delete(id: String)
    suspend fun getListings(sellerId: String?): List<SupabaseListingDto>
    suspend fun authenticatedUserId(): String?
}

private class PostgrestSupabaseListingsTable(
    private val supabase: SupabaseClient
) : SupabaseListingsTable {
    override suspend fun findById(id: String): SupabaseListingDto? {
        return supabase.from(SupabaseListingsRepository.LISTINGS_TABLE).select {
            filter { eq("id", id) }
        }.decodeSingleOrNull()
    }

    override suspend fun insert(listing: SupabaseListingInsertDto) {
        supabase.from(SupabaseListingsRepository.LISTINGS_TABLE).insert(listing)
    }

    override suspend fun delete(id: String) {
        supabase.from(SupabaseListingsRepository.LISTINGS_TABLE).delete {
            filter { eq("id", id) }
        }
    }

    override suspend fun getListings(sellerId: String?): List<SupabaseListingDto> {
        return supabase.from(SupabaseListingsRepository.LISTINGS_TABLE).select {
            sellerId?.let { filter { eq("user_id", it) } }
        }.decodeList()
    }

    override suspend fun authenticatedUserId(): String? = supabase.auth.currentUserOrNull()?.id
}
