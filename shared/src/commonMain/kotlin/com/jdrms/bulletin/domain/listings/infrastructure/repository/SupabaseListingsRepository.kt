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
                return@runCatching Result.Error(IllegalStateException(CreateListingErrorMessages.GENERIC_FAILURE))
            }
            listingsTable.insert(SupabaseListingMapper.toInsertDto(listing, sellerId))
            listing.copy(sellerId = SellerId(sellerId))
        }.getOrElse { error ->
            Result.Error(Exception(mapListingsErrorMessage(error), error))
        }
    }

    override suspend fun updateListing(listing: Listing): Result<Listing> {
        return Result.Error(UnsupportedOperationException("Listing updates are not implemented for Supabase."))
    }

    override suspend fun deleteListing(id: ListingId): Result<Unit> {
        return try {
            val existing = listingsTable.findById(id.value)
                ?: return Result.Error(NoSuchElementException("Listing not found."))
            requireAuthenticatedSeller(existing.userId.orEmpty())
            listingsTable.delete(id.value)
            Result.Success(Unit)
        } catch (error: Throwable) {
            Result.Error(Exception(mapListingsErrorMessage(error), error))
        }
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

        private val ERROR_RULES = listOf(
            listOf("could not find the table", "schema cache") to
                "Database table 'listings' not found. Please verify your Supabase schema setup.",
            listOf("unable to resolve host", "failed to connect", "timeout", "request timeout") to
                "Unable to connect to server. Please check your internet connection.",
            listOf("jwt", "unauthorized", "invalid api key", "no api key") to
                "Unauthorized database request. Please check your Supabase API credentials.",
            listOf("row-level security", "rls") to
                "Database permission denied. Please check your Supabase RLS policies."
        )

        fun mapListingsErrorMessage(throwable: Throwable): String {
            val message = throwable.message ?: return "An unexpected listings error occurred."
            val lower = message.lowercase()
            ERROR_RULES.firstOrNull { (patterns, _) -> patterns.any(lower::contains) }?.let { return it.second }
            return message.lines().firstOrNull { it.isNotBlank() }?.trim() ?: "Listings request failed."
        }
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
